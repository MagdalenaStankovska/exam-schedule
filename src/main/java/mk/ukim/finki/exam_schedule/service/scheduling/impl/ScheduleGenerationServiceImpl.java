package mk.ukim.finki.exam_schedule.service.scheduling.impl;

import jakarta.transaction.Transactional;
import mk.ukim.finki.exam_schedule.model.*;
import mk.ukim.finki.exam_schedule.model.exceptions.InvalidYearExamSessionException;
import mk.ukim.finki.exam_schedule.repository.StudentCoursesRepository;
import mk.ukim.finki.exam_schedule.repository.SubjectExamRepository;
import mk.ukim.finki.exam_schedule.repository.TeacherSubjectAllocationsRepository;
import mk.ukim.finki.exam_schedule.repository.TimeSlotRepository;
import mk.ukim.finki.exam_schedule.repository.YearExamSessionRepository;
import mk.ukim.finki.exam_schedule.service.RoomService;
import mk.ukim.finki.exam_schedule.service.scheduling.GeminiScheduleClient;
import mk.ukim.finki.exam_schedule.service.scheduling.GeminiSuggestion;
import mk.ukim.finki.exam_schedule.service.scheduling.ScheduleGenerationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleGenerationServiceImpl implements ScheduleGenerationService {

    private static final int SLOT_MINUTES = 15;
    private static final int DAY_START_HOUR = 8;
    private static final int DAY_END_HOUR = 21;

    private final SubjectExamRepository subjectExamRepository;
    private final YearExamSessionRepository yearExamSessionRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final TeacherSubjectAllocationsRepository teacherSubjectAllocationsRepository;
    private final StudentCoursesRepository studentCoursesRepository;
    private final RoomService roomService;
    private final GeminiScheduleClient geminiScheduleClient;

    public ScheduleGenerationServiceImpl(SubjectExamRepository subjectExamRepository,
                                         YearExamSessionRepository yearExamSessionRepository,
                                         TimeSlotRepository timeSlotRepository,
                                         TeacherSubjectAllocationsRepository teacherSubjectAllocationsRepository,
                                         StudentCoursesRepository studentCoursesRepository,
                                         RoomService roomService,
                                         GeminiScheduleClient geminiScheduleClient) {
        this.subjectExamRepository = subjectExamRepository;
        this.yearExamSessionRepository = yearExamSessionRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.teacherSubjectAllocationsRepository = teacherSubjectAllocationsRepository;
        this.studentCoursesRepository = studentCoursesRepository;
        this.roomService = roomService;
        this.geminiScheduleClient = geminiScheduleClient;
    }

    @Override
    @Transactional(dontRollbackOn = IllegalStateException.class)
    public List<SubjectExam> generateForSession(String yearExamSessionName) {
        YearExamSession session = yearExamSessionRepository.findById(yearExamSessionName)
                .orElseThrow(InvalidYearExamSessionException::new);

        if (session.getSessionStart() == null || session.getSessionEnd() == null) {
            throw new IllegalStateException("Selected exam session has no start/end dates.");
        }

        List<SubjectExam> allSessionExams = subjectExamRepository.findAllBySession(session);
        List<SubjectExam> exams = allSessionExams.stream()
                .filter(this::isEligibleForGeneration)
                .sorted(Comparator.comparingLong(this::priorityScore).reversed())
                .collect(Collectors.toList());

        if (exams.isEmpty()) {
            session.setSchedulingTriggeredAt(LocalDateTime.now());
            yearExamSessionRepository.save(session);
            return Collections.emptyList();
        }

        Set<String> mutableExamIds = exams.stream().map(SubjectExam::getId).collect(Collectors.toSet());
        List<SubjectExam> fixedScheduledExams = allSessionExams.stream()
                .filter(exam -> !mutableExamIds.contains(exam.getId()))
                .filter(exam -> exam.getFromTime() != null && exam.getToTime() != null)
                .toList();

        Map<String, Set<String>> subjectToProfessorIds = buildSubjectProfessorIndex(allSessionExams);
        Map<String, Set<String>> subjectToStudentIndexes = buildSubjectStudentIndex(allSessionExams);
        Map<String, List<SubjectExam>> byRoom = buildRoomUsageIndex(fixedScheduledExams);
        List<SubjectExam> scheduledForConflicts = new ArrayList<>(fixedScheduledExams);

        Map<String, GeminiSuggestion> geminiSuggestions = geminiScheduleClient.suggestSchedule(yearExamSessionName, exams).stream()
                .filter(suggestion -> suggestion.subjectExamId() != null && !suggestion.subjectExamId().isBlank())
                .collect(Collectors.toMap(GeminiSuggestion::subjectExamId, suggestion -> suggestion, (first, second) -> first));

        List<String> failedExamIds = new ArrayList<>();

        for (SubjectExam exam : exams) {
            clearCurrentPlacement(exam);

            boolean placed = needsRoom(exam)
                    ? tryPlaceRoomBasedExam(session, exam, scheduledForConflicts, byRoom,
                    subjectToProfessorIds, subjectToStudentIndexes, geminiSuggestions.get(exam.getId()))
                    : tryPlaceExamWithoutRooms(session, exam, scheduledForConflicts,
                    subjectToProfessorIds, subjectToStudentIndexes, geminiSuggestions.get(exam.getId()));

            if (!placed) {
                failedExamIds.add(exam.getId());
                appendCommentOnce(exam,
                        "AUTO-SCHEDULING FAILED",
                        "AUTO-SCHEDULING FAILED: no valid room/time combination under current constraints.");
                continue;
            }

            scheduledForConflicts.add(exam);
            registerRoomUsage(byRoom, exam);
        }

        session.setSchedulingTriggeredAt(LocalDateTime.now());
        yearExamSessionRepository.save(session);

        List<SubjectExam> saved = subjectExamRepository.saveAll(exams);
        if (!failedExamIds.isEmpty()) {
            throw new IllegalStateException("Generated partially. Could not place exams: " + String.join(", ", failedExamIds));
        }

        return saved;
    }

    private boolean tryPlaceRoomBasedExam(YearExamSession session,
                                          SubjectExam exam,
                                          List<SubjectExam> scheduledForConflicts,
                                          Map<String, List<SubjectExam>> byRoom,
                                          Map<String, Set<String>> subjectToProfessorIds,
                                          Map<String, Set<String>> subjectToStudentIndexes,
                                          GeminiSuggestion suggestion) {
        List<Room> candidateRooms = roomsByExamType(exam.getDefinition().getType()).stream()
                .filter(room -> room.getCapacity() != null && room.getCapacity() > 0)
                .filter(room -> roomMatchesEquipmentRequirements(exam, room))
                .sorted(Comparator.comparing(Room::getCapacity).reversed())
                .toList();

        if (candidateRooms.isEmpty()) {
            return false;
        }

        long totalCapacityPerRound = candidateRooms.stream()
                .mapToLong(room -> Optional.ofNullable(room.getCapacity()).orElse(0L))
                .sum();
        if (totalCapacityPerRound <= 0) {
            return false;
        }

        long expectedStudents = Math.max(1L, expected(exam));
        long minRoundsNeeded = (long) Math.ceil((double) expectedStudents / totalCapacityPerRound);
        long rounds = Math.max(Optional.ofNullable(exam.getNumRepetitions()).orElse(1L), minRoundsNeeded);
        long requiredCapacityForOnePlacement = (long) Math.ceil((double) expectedStudents / rounds);

        if (tryApplyGeminiRoomSuggestion(session, exam, suggestion, candidateRooms, byRoom, scheduledForConflicts,
                subjectToProfessorIds, subjectToStudentIndexes, requiredCapacityForOnePlacement, expectedStudents, rounds)) {
            return true;
        }

        PlacementCandidate best = null;
        for (LocalDateTime slotStart : slotStartsForSession(session, duration(exam))) {
            LocalDateTime slotEnd = slotStart.plusMinutes(duration(exam));

            if (violatesHardConflicts(exam, slotStart, slotEnd, scheduledForConflicts, subjectToProfessorIds, subjectToStudentIndexes)) {
                continue;
            }

            Optional<Set<Room>> selectedRooms = selectRoomsForSlot(candidateRooms, byRoom, slotStart, slotEnd,
                    requiredCapacityForOnePlacement);
            if (selectedRooms.isEmpty()) {
                continue;
            }

            long score = placementScore(exam, slotStart, slotEnd, scheduledForConflicts);
            if (best == null || score < best.score()) {
                best = new PlacementCandidate(slotStart, slotEnd, selectedRooms.get(), score);
            }
        }

        if (best == null) {
            return false;
        }

        setExamSchedule(exam, best.start(), best.end(), best.rooms());
        applyCapacityPlanningMetadata(exam, best.rooms(), expectedStudents, rounds, requiredCapacityForOnePlacement, "AUTO");
        return true;
    }

    private boolean tryPlaceExamWithoutRooms(YearExamSession session,
                                             SubjectExam exam,
                                             List<SubjectExam> scheduledForConflicts,
                                             Map<String, Set<String>> subjectToProfessorIds,
                                             Map<String, Set<String>> subjectToStudentIndexes,
                                             GeminiSuggestion suggestion) {
        if (tryApplyGeminiNonRoomSuggestion(session, exam, suggestion, scheduledForConflicts,
                subjectToProfessorIds, subjectToStudentIndexes)) {
            return true;
        }

        PlacementCandidate best = null;
        for (LocalDateTime slotStart : slotStartsForSession(session, duration(exam))) {
            LocalDateTime slotEnd = slotStart.plusMinutes(duration(exam));
            if (violatesHardConflicts(exam, slotStart, slotEnd, scheduledForConflicts, subjectToProfessorIds, subjectToStudentIndexes)) {
                continue;
            }

            long score = placementScore(exam, slotStart, slotEnd, scheduledForConflicts);
            if (best == null || score < best.score()) {
                best = new PlacementCandidate(slotStart, slotEnd, Collections.emptySet(), score);
            }
        }

        if (best == null) {
            return false;
        }

        setExamSchedule(exam, best.start(), best.end(), Collections.emptySet());
        exam.setNumRepetitions(1L);
        return true;
    }

    private boolean tryApplyGeminiRoomSuggestion(YearExamSession session,
                                                  SubjectExam exam,
                                                  GeminiSuggestion suggestion,
                                                  List<Room> candidateRooms,
                                                  Map<String, List<SubjectExam>> byRoom,
                                                  List<SubjectExam> scheduledForConflicts,
                                                  Map<String, Set<String>> subjectToProfessorIds,
                                                  Map<String, Set<String>> subjectToStudentIndexes,
                                                  long requiredCapacity,
                                                  long expectedStudents,
                                                  long rounds) {
        if (suggestion == null || suggestion.fromTime() == null || suggestion.toTime() == null) {
            return false;
        }

        if (!isInsideSessionBoundaries(session, suggestion.fromTime(), suggestion.toTime())) {
            return false;
        }

        if (violatesHardConflicts(exam, suggestion.fromTime(), suggestion.toTime(), scheduledForConflicts,
                subjectToProfessorIds, subjectToStudentIndexes)) {
            return false;
        }

        Map<String, Room> candidateByName = candidateRooms.stream()
                .collect(Collectors.toMap(Room::getName, room -> room, (first, second) -> first));
        Set<Room> selectedRooms = new LinkedHashSet<>();

        if (suggestion.roomNames() != null) {
            for (String roomName : suggestion.roomNames()) {
                Room room = candidateByName.get(roomName);
                if (room == null) {
                    continue;
                }
                if (hasRoomOverlap(byRoom, room.getName(), suggestion.fromTime(), suggestion.toTime())) {
                    return false;
                }
                selectedRooms.add(room);
            }
        }

        if (selectedRooms.isEmpty()) {
            return false;
        }

        long offeredCapacity = selectedRooms.stream()
                .mapToLong(room -> Optional.ofNullable(room.getCapacity()).orElse(0L))
                .sum();
        if (offeredCapacity < requiredCapacity) {
            return false;
        }

        setExamSchedule(exam, suggestion.fromTime(), suggestion.toTime(), selectedRooms);
        applyCapacityPlanningMetadata(exam, selectedRooms, expectedStudents, rounds, requiredCapacity, "AUTO-GEMINI");
        appendCommentOnce(exam, "AUTO-GEMINI", "AUTO-GEMINI: accepted suggested room/time placement.");
        return true;
    }

    private boolean tryApplyGeminiNonRoomSuggestion(YearExamSession session,
                                                     SubjectExam exam,
                                                     GeminiSuggestion suggestion,
                                                     List<SubjectExam> scheduledForConflicts,
                                                     Map<String, Set<String>> subjectToProfessorIds,
                                                     Map<String, Set<String>> subjectToStudentIndexes) {
        if (suggestion == null || suggestion.fromTime() == null || suggestion.toTime() == null) {
            return false;
        }

        if (!isInsideSessionBoundaries(session, suggestion.fromTime(), suggestion.toTime())) {
            return false;
        }

        if (violatesHardConflicts(exam, suggestion.fromTime(), suggestion.toTime(), scheduledForConflicts,
                subjectToProfessorIds, subjectToStudentIndexes)) {
            return false;
        }

        setExamSchedule(exam, suggestion.fromTime(), suggestion.toTime(), Collections.emptySet());
        exam.setNumRepetitions(1L);
        appendCommentOnce(exam, "AUTO-GEMINI", "AUTO-GEMINI: accepted suggested timeslot.");
        return true;
    }

    private Optional<Set<Room>> selectRoomsForSlot(List<Room> candidateRooms,
                                                   Map<String, List<SubjectExam>> byRoom,
                                                   LocalDateTime slotStart,
                                                   LocalDateTime slotEnd,
                                                   long requiredCapacity) {
        List<Room> availableRooms = candidateRooms.stream()
                .filter(room -> !hasRoomOverlap(byRoom, room.getName(), slotStart, slotEnd))
                .toList();

        if (availableRooms.isEmpty()) {
            return Optional.empty();
        }

        long covered = 0L;
        Set<Room> selected = new LinkedHashSet<>();

        for (Room room : availableRooms) {
            selected.add(room);
            covered += Optional.ofNullable(room.getCapacity()).orElse(0L);
            if (covered >= requiredCapacity) {
                return Optional.of(selected);
            }
        }

        return Optional.empty();
    }

    private Map<String, List<SubjectExam>> buildRoomUsageIndex(List<SubjectExam> exams) {
        Map<String, List<SubjectExam>> byRoom = new HashMap<>();
        for (SubjectExam exam : exams) {
            registerRoomUsage(byRoom, exam);
        }
        return byRoom;
    }

    private void registerRoomUsage(Map<String, List<SubjectExam>> byRoom, SubjectExam exam) {
        if (exam.getRooms() == null) {
            return;
        }
        for (Room room : exam.getRooms()) {
            if (room == null || room.getName() == null || room.getName().isBlank()) {
                continue;
            }
            byRoom.computeIfAbsent(room.getName(), key -> new ArrayList<>()).add(exam);
        }
    }

    private Map<String, Set<String>> buildSubjectProfessorIndex(List<SubjectExam> exams) {
        Set<String> subjectIds = exams.stream()
                .map(this::subjectIdOf)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (subjectIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Set<String>> subjectToProfessorIds = new HashMap<>();
        teacherSubjectAllocationsRepository.findAllBySubjectIdIn(subjectIds).forEach(allocation -> {
            if (allocation.getSubjectId() == null || allocation.getSubjectId().isBlank() ||
                    allocation.getProfessorId() == null || allocation.getProfessorId().isBlank()) {
                return;
            }
            subjectToProfessorIds.computeIfAbsent(allocation.getSubjectId(), key -> new HashSet<>())
                    .add(allocation.getProfessorId());
        });

        return subjectToProfessorIds;
    }

    private Map<String, Set<String>> buildSubjectStudentIndex(List<SubjectExam> exams) {
        Set<String> subjectIds = exams.stream()
                .map(this::subjectIdOf)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (subjectIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Set<String>> subjectToStudents = new HashMap<>();
        studentCoursesRepository.findAllByCourse_JoinedSubject_AbbreviationIn(subjectIds).forEach(link -> {
            if (link == null || link.getStudent() == null || link.getCourse() == null ||
                    link.getCourse().getJoinedSubject() == null || link.getStudent().getIndex() == null) {
                return;
            }
            String subjectId = link.getCourse().getJoinedSubject().getAbbreviation();
            subjectToStudents.computeIfAbsent(subjectId, key -> new HashSet<>())
                    .add(link.getStudent().getIndex());
        });

        return subjectToStudents;
    }

    private boolean violatesHardConflicts(SubjectExam exam,
                                          LocalDateTime start,
                                          LocalDateTime end,
                                          List<SubjectExam> scheduledForConflicts,
                                          Map<String, Set<String>> subjectToProfessorIds,
                                          Map<String, Set<String>> subjectToStudentIndexes) {
        for (SubjectExam existing : scheduledForConflicts) {
            if (existing.getFromTime() == null || existing.getToTime() == null) {
                continue;
            }
            if (existing.getId().equals(exam.getId())) {
                continue;
            }
            if (!areOverlapping(start, end, existing.getFromTime(), existing.getToTime())) {
                continue;
            }

            if (sameCycleAndSemester(exam, existing)) {
                return true;
            }

            Set<String> examProfessors = professorIds(exam, subjectToProfessorIds);
            Set<String> existingProfessors = professorIds(existing, subjectToProfessorIds);
            if (intersects(examProfessors, existingProfessors)) {
                return true;
            }

            Set<String> examStudents = studentIndexes(exam, subjectToStudentIndexes);
            Set<String> existingStudents = studentIndexes(existing, subjectToStudentIndexes);
            if (intersects(examStudents, existingStudents)) {
                return true;
            }
        }

        return false;
    }

    private long placementScore(SubjectExam exam,
                                LocalDateTime start,
                                LocalDateTime end,
                                List<SubjectExam> scheduledForConflicts) {
        long score = 0L;

        LocalDate date = start.toLocalDate();
        long dayLoad = scheduledForConflicts.stream()
                .filter(existing -> existing.getFromTime() != null)
                .filter(existing -> existing.getFromTime().toLocalDate().equals(date))
                .count();
        score += dayLoad * 25L;

        long timePenalty = ChronoUnit.MINUTES.between(date.atTime(DAY_START_HOUR, 0), start) / SLOT_MINUTES;
        long priority = priorityScore(exam);
        score += Math.max(1L, 700L - Math.min(700L, priority)) * timePenalty / 100L;

        long sameCohortOnDay = scheduledForConflicts.stream()
                .filter(existing -> existing.getFromTime() != null && existing.getToTime() != null)
                .filter(existing -> sameCycleAndSemester(exam, existing))
                .filter(existing -> existing.getFromTime().toLocalDate().equals(date))
                .count();
        score += sameCohortOnDay * 40L;

        if (isCoreOrDifficult(exam)) {
            boolean hasAdjacentDifficult = scheduledForConflicts.stream()
                    .filter(existing -> existing.getFromTime() != null)
                    .filter(this::isCoreOrDifficult)
                    .filter(existing -> sameCycleAndSemester(exam, existing))
                    .anyMatch(existing -> Math.abs(ChronoUnit.DAYS.between(existing.getFromTime().toLocalDate(), date)) <= 1);
            if (hasAdjacentDifficult) {
                score += 180L;
            }
        }

        long nearestGapMinutes = scheduledForConflicts.stream()
                .filter(existing -> existing.getFromTime() != null && existing.getToTime() != null)
                .filter(existing -> sameCycleAndSemester(exam, existing))
                .filter(existing -> existing.getFromTime().toLocalDate().equals(date))
                .mapToLong(existing -> {
                    if (existing.getToTime().isBefore(start)) {
                        return ChronoUnit.MINUTES.between(existing.getToTime(), start);
                    }
                    if (end.isBefore(existing.getFromTime())) {
                        return ChronoUnit.MINUTES.between(end, existing.getFromTime());
                    }
                    return 0L;
                })
                .filter(value -> value > 0L)
                .min()
                .orElse(0L);
        if (nearestGapMinutes >= 180L) {
            score += 40L;
        } else if (nearestGapMinutes >= 60L) {
            score += 10L;
        }

        return score;
    }

    private List<LocalDateTime> slotStartsForSession(YearExamSession session, long durationMinutes) {
        List<LocalDateTime> starts = new ArrayList<>();

        for (LocalDate date = session.getSessionStart(); !date.isAfter(session.getSessionEnd()); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atTime(DAY_START_HOUR, 0);
            LocalDateTime dayEnd = date.atTime(DAY_END_HOUR, 0);
            for (LocalDateTime slotStart = dayStart;
                 !slotStart.plusMinutes(durationMinutes).isAfter(dayEnd);
                 slotStart = slotStart.plusMinutes(SLOT_MINUTES)) {
                starts.add(slotStart);
            }
        }

        return starts;
    }

    private boolean isInsideSessionBoundaries(YearExamSession session, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            return false;
        }
        LocalDateTime sessionStart = session.getSessionStart().atTime(DAY_START_HOUR, 0);
        LocalDateTime sessionEnd = session.getSessionEnd().atTime(DAY_END_HOUR, 0);
        return !start.isBefore(sessionStart) && !end.isAfter(sessionEnd);
    }

    private boolean sameCycleAndSemester(SubjectExam first, SubjectExam second) {
        if (first.getDefinition() == null || second.getDefinition() == null ||
                first.getDefinition().getSubject() == null || second.getDefinition().getSubject() == null) {
            return false;
        }

        JoinedSubject firstSubject = first.getDefinition().getSubject();
        JoinedSubject secondSubject = second.getDefinition().getSubject();
        return Objects.equals(first.getSession().getCycle(), second.getSession().getCycle())
                && firstSubject.getSemesterType() == secondSubject.getSemesterType();
    }

    private boolean hasRoomOverlap(Map<String, List<SubjectExam>> byRoom, String roomName,
                                   LocalDateTime start, LocalDateTime end) {
        return byRoom.getOrDefault(roomName, Collections.emptyList()).stream()
                .filter(exam -> exam.getFromTime() != null && exam.getToTime() != null)
                .anyMatch(exam -> areOverlapping(start, end, exam.getFromTime(), exam.getToTime()));
    }

    private boolean areOverlapping(LocalDateTime start1, LocalDateTime end1,
                                   LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private boolean intersects(Set<String> first, Set<String> second) {
        if (first == null || first.isEmpty() || second == null || second.isEmpty()) {
            return false;
        }
        Set<String> smaller = first.size() <= second.size() ? first : second;
        Set<String> larger = first.size() <= second.size() ? second : first;
        for (String value : smaller) {
            if (larger.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> professorIds(SubjectExam exam, Map<String, Set<String>> subjectToProfessorIds) {
        String subjectId = subjectIdOf(exam);
        if (subjectId == null) {
            return Collections.emptySet();
        }
        return subjectToProfessorIds.getOrDefault(subjectId, Collections.emptySet());
    }

    private Set<String> studentIndexes(SubjectExam exam, Map<String, Set<String>> subjectToStudents) {
        String subjectId = subjectIdOf(exam);
        if (subjectId == null) {
            return Collections.emptySet();
        }
        return subjectToStudents.getOrDefault(subjectId, Collections.emptySet());
    }

    private String subjectIdOf(SubjectExam exam) {
        if (exam == null || exam.getDefinition() == null || exam.getDefinition().getSubject() == null) {
            return null;
        }
        return exam.getDefinition().getSubject().getAbbreviation();
    }

    private void clearCurrentPlacement(SubjectExam exam) {
        exam.setFromTime(null);
        exam.setToTime(null);
        exam.setTimeSlot(null);
        exam.setRooms(new HashSet<>());
    }

    private void setExamSchedule(SubjectExam exam, LocalDateTime fromTime, LocalDateTime toTime, Set<Room> rooms) {
        TimeSlot slot = timeSlotRepository.save(new TimeSlot(fromTime, toTime));
        exam.setFromTime(fromTime);
        exam.setToTime(toTime);
        exam.setTimeSlot(slot);
        exam.setRooms(new HashSet<>(Optional.ofNullable(rooms).orElseGet(Collections::emptySet)));
        exam.setWorkflowStatus(ExamWorkflowStatus.SCHEDULED);
    }

    private void applyCapacityPlanningMetadata(SubjectExam exam,
                                               Set<Room> selectedRooms,
                                               long expectedStudents,
                                               long rounds,
                                               long requiredCapacityForOnePlacement,
                                               String sourceTag) {
        exam.setNumRepetitions(rounds);
        if (rounds > 1) {
            appendCommentOnce(exam,
                    sourceTag + " REPETITIONS",
                    sourceTag + ": Planned repetitions=" + rounds +
                            " (capacity per placement=" + requiredCapacityForOnePlacement + ").");
        }

        appendCommentOnce(exam,
                sourceTag + " SPLIT",
                buildSplitPlanComment(sourceTag, selectedRooms, expectedStudents, requiredCapacityForOnePlacement, rounds));
    }

    private String buildSplitPlanComment(String sourceTag,
                                         Set<Room> selectedRooms,
                                         long expectedStudents,
                                         long requiredCapacityForOnePlacement,
                                         long rounds) {
        List<Room> orderedRooms = selectedRooms.stream()
                .sorted(Comparator.comparing(Room::getName))
                .toList();

        long remaining = requiredCapacityForOnePlacement;
        List<String> split = new ArrayList<>();
        for (Room room : orderedRooms) {
            long roomCapacity = Optional.ofNullable(room.getCapacity()).orElse(0L);
            long assigned = Math.max(0L, Math.min(roomCapacity, remaining));
            split.add(room.getName() + "=" + assigned + "/" + roomCapacity);
            remaining -= assigned;
        }

        return sourceTag + ": same-time multi-room split (expected=" + expectedStudents +
                ", rounds=" + rounds + ", per-placement-target=" + requiredCapacityForOnePlacement +
                ") => " + String.join(", ", split);
    }

    private void appendCommentOnce(SubjectExam exam, String uniqueMarker, String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        String comment = exam.getComment();
        if (comment != null && comment.contains(uniqueMarker)) {
            return;
        }

        if (comment == null || comment.isBlank()) {
            exam.setComment(line);
        } else {
            exam.setComment(comment + "\n" + line);
        }
    }

    private long duration(SubjectExam exam) {
        long definitionDuration = exam.getDefinition() == null ? 60L : Optional.ofNullable(exam.getDefinition().getDurationMinutes()).orElse(60L);
        long base = Optional.ofNullable(exam.getDurationMinutes()).orElse(definitionDuration);
        // Align to 15-minute intervals: round up to nearest 15.
        return Math.max(15, ((base + 14) / 15) * 15);
    }

    private long expected(SubjectExam exam) {
        return Optional.ofNullable(exam.getExpectedNumber()).orElse(0L);
    }

    private long priorityScore(SubjectExam exam) {
        long score = expected(exam) * 10L;

        if (isCoreOrDifficult(exam)) {
            score += 1_000L;
        }

        String note = exam.getDefinition() == null || exam.getDefinition().getNote() == null
                ? ""
                : exam.getDefinition().getNote().toLowerCase(Locale.ROOT);
        if (note.contains("final-year") || note.contains("senior") || note.contains("capstone")) {
            score += 800L;
        }

        String subjectName = exam.getDefinition() != null && exam.getDefinition().getSubject() != null
                ? Optional.ofNullable(exam.getDefinition().getSubject().getName()).orElse("").toLowerCase(Locale.ROOT)
                : "";
        if (subjectName.contains("algorithms") || subjectName.contains("database") || subjectName.contains("operating systems")) {
            score += 400L;
        }

        if (exam.getSession() != null && exam.getSession().getCycle() != null &&
                (exam.getSession().getCycle().contains(StudyCycle.MASTER) || exam.getSession().getCycle().contains(StudyCycle.PHD))) {
            score += 250L;
        }

        return score;
    }

    private boolean isCoreOrDifficult(SubjectExam exam) {
        String note = exam.getDefinition() == null || exam.getDefinition().getNote() == null
                ? ""
                : exam.getDefinition().getNote().toLowerCase(Locale.ROOT);
        String subjectName = exam.getDefinition() != null && exam.getDefinition().getSubject() != null
                ? Optional.ofNullable(exam.getDefinition().getSubject().getName()).orElse("").toLowerCase(Locale.ROOT)
                : "";
        return note.contains("core") || note.contains("difficult") || note.contains("hard") ||
                subjectName.contains("advanced") || subjectName.contains("core");
    }

    private boolean roomMatchesEquipmentRequirements(SubjectExam exam, Room room) {
        Set<String> equipmentTags = requiredEquipmentTags(exam);
        if (equipmentTags.isEmpty()) {
            return true;
        }
        String roomEquipment = Optional.ofNullable(room.getEquipmentDescription()).orElse("").toLowerCase(Locale.ROOT);
        for (String tag : equipmentTags) {
            if (!roomEquipment.contains(tag)) {
                return false;
            }
        }
        return true;
    }

    private Set<String> requiredEquipmentTags(SubjectExam exam) {
        String note = exam.getDefinition() == null ? null : exam.getDefinition().getNote();
        if (note == null || note.isBlank()) {
            return Collections.emptySet();
        }

        String lower = note.toLowerCase(Locale.ROOT);
        int markerIndex = lower.indexOf("equipment:");
        if (markerIndex < 0) {
            return Collections.emptySet();
        }

        String rawEquipment = lower.substring(markerIndex + "equipment:".length()).trim();
        int stopAt = rawEquipment.indexOf(';');
        if (stopAt >= 0) {
            rawEquipment = rawEquipment.substring(0, stopAt);
        }

        Set<String> tags = new HashSet<>();
        for (String part : rawEquipment.split(",")) {
            String tag = part.trim();
            if (!tag.isBlank()) {
                tags.add(tag);
            }
        }

        return tags;
    }

    private boolean isEligibleForGeneration(SubjectExam exam) {
        ExamWorkflowStatus status = exam.getWorkflowStatus();
        if (status == null) {
            // Legacy rows may have null status; treat them as draft for recovery.
            return true;
        }
        if (status == ExamWorkflowStatus.FINALIZED) {
            // Allow recovery for mistakenly finalized but incomplete exams.
            return !isFullyScheduled(exam);
        }
        if (status == ExamWorkflowStatus.SCHEDULED) {
            // Re-run only if still incomplete (e.g., no room assigned).
            return !isFullyScheduled(exam);
        }
        return status == ExamWorkflowStatus.DRAFT || status == ExamWorkflowStatus.SUBMITTED;
    }

    private boolean isFullyScheduled(SubjectExam exam) {
        if (exam.getFromTime() == null || exam.getToTime() == null) {
            return false;
        }
        if (!needsRoom(exam)) {
            return true;
        }
        return exam.getRooms() != null && !exam.getRooms().isEmpty();
    }

    private boolean needsRoom(SubjectExam exam) {
        if (exam.getDefinition() == null || exam.getDefinition().getType() == null) {
            return false;
        }
        ExamType type = exam.getDefinition().getType();
        return type == ExamType.CLASSROOM || type == ExamType.LAB;
    }

    private List<Room> roomsByExamType(ExamType type) {
        if (type == ExamType.LAB) {
            return roomService.findAllByRoomType(RoomType.LAB);
        }
        if (type == ExamType.CLASSROOM) {
            return roomService.findAllByRoomType(RoomType.CLASSROOM);
        }
        return Collections.emptyList();
    }

    private record PlacementCandidate(LocalDateTime start,
                                      LocalDateTime end,
                                      Set<Room> rooms,
                                      long score) {
    }
}

