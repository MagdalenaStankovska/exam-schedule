package mk.ukim.finki.exam_schedule.service.impl;

import mk.ukim.finki.exam_schedule.model.ExamDefinition;
import mk.ukim.finki.exam_schedule.model.Room;
import mk.ukim.finki.exam_schedule.model.SubjectExam;
import mk.ukim.finki.exam_schedule.model.YearExamSession;
import mk.ukim.finki.exam_schedule.model.exceptions.InvalidYearExamSessionException;
import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.exam_schedule.model.*;
import mk.ukim.finki.exam_schedule.model.exceptions.OverlappingExamTimesInTheSameRoomException;
import mk.ukim.finki.exam_schedule.model.exceptions.SubjectExamNotFoundException;
import mk.ukim.finki.exam_schedule.repository.ExamDefinitionRepository;
import mk.ukim.finki.exam_schedule.repository.JoinedSubjectRepository;
import mk.ukim.finki.exam_schedule.repository.SubjectExamRepository;
import mk.ukim.finki.exam_schedule.repository.TeacherSubjectAllocationsRepository;
import mk.ukim.finki.exam_schedule.repository.TimeSlotRepository;
import mk.ukim.finki.exam_schedule.repository.YearExamSessionRepository;
import mk.ukim.finki.exam_schedule.service.CurrentUserService;
import mk.ukim.finki.exam_schedule.service.RoomService;
import mk.ukim.finki.exam_schedule.service.SubjectAllocationStatsService;
import mk.ukim.finki.exam_schedule.service.SubjectExamService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SubjectExamServiceImpl implements SubjectExamService {

    private final SubjectExamRepository subjectExamRepository;
    private final YearExamSessionRepository yearExamSessionRepository;
    private final ExamDefinitionRepository examDefinitionRepository;
    private final JoinedSubjectRepository joinedSubjectRepository;
    private final SubjectAllocationStatsService subjectAllocationStatsService;
    private final TeacherSubjectAllocationsRepository teacherSubjectAllocationsRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final CurrentUserService currentUserService;

    private final RoomService roomService;

    public SubjectExamServiceImpl(SubjectExamRepository subjectExamRepository, YearExamSessionRepository yearExamSessionRepository, ExamDefinitionRepository examDefinitionRepository, JoinedSubjectRepository joinedSubjectRepository, SubjectAllocationStatsService subjectAllocationStatsService, TeacherSubjectAllocationsRepository teacherSubjectAllocationsRepository, TimeSlotRepository timeSlotRepository, CurrentUserService currentUserService, RoomService roomService) {
        this.subjectExamRepository = subjectExamRepository;
        this.yearExamSessionRepository = yearExamSessionRepository;
        this.examDefinitionRepository = examDefinitionRepository;
        this.joinedSubjectRepository = joinedSubjectRepository;
        this.subjectAllocationStatsService = subjectAllocationStatsService;
        this.teacherSubjectAllocationsRepository = teacherSubjectAllocationsRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.currentUserService = currentUserService;
        this.roomService = roomService;
    }

    @Override
    public SubjectExam findByName(String name) {
        return this.subjectExamRepository.findById(name).orElseThrow(SubjectExamNotFoundException::new);
    }

    @Override
    public Page<SubjectExam> findAll(Specification<SubjectExam> filter, Integer page, Integer size) {
        return this.subjectExamRepository.findAll(filter, PageRequest.of(page - 1, size,
                Sort.by(Sort.Direction.DESC, "session.year")
                        .and(Sort.by(Sort.Direction.DESC, "fromTime"))
                        .and(Sort.by(Sort.Direction.DESC, "toTime"))));
    }

    @Override
    public List<SubjectExam> findAllByExamDefinitionAndExamSession(ExamDefinition examDefinition, ExamSession examSession) {
        return subjectExamRepository.findAllByDefinitionAndSessionSession(examDefinition, examSession);
    }

    @Override
    public SubjectExam create(YearExamSession session, ExamDefinition definition) {
        return this.subjectExamRepository.save(new SubjectExam(definition, session));
    }

    @Override
    public void initialize(String yes) {
        List<ExamDefinition> examDefinitions = this.examDefinitionRepository.findAll();
        YearExamSession yearExamSession = this.yearExamSessionRepository.findById(yes).orElseThrow(InvalidYearExamSessionException::new);
        examDefinitions.stream().filter(e -> e.getExamSession() == yearExamSession.getSession())
                .forEach(e -> {
                    String id = String.format("%s-%s", yes, e.getId());
                    if (subjectExamRepository.findById(id).isEmpty()) {
                        SubjectExam newExam = CheckPreviousYear(id, e, yearExamSession);
                        subjectExamRepository.save(newExam);
                    }
                });

    }

    @Override
    public Set<Room> getAllRooms() {
        return new HashSet<>(roomService.findAll());
    }

    @Override
    public Set<Room> getRoomsByNames(Set<String> roomNames) {
        if (roomNames != null) return roomService.findAllByNameIn(roomNames);
        return new HashSet<>();
    }

    @Override
    public SubjectExam update(String name, YearExamSession session, Long durationMinutes,
                              Long previousYearAttendantsNumber, Long previousYearTotalStudents,
                              Long attendantsNumber, Long totalStudents, Long expectedNumber,
                              Long numRepetitions, LocalDateTime fromTime, LocalDateTime toTime,
                              Set<String> roomNames, String comment) {
        SubjectExam existing = this.findByName(name);
        if (!canManageExam(existing)) {
            return null;
        }
        Set<Room> rooms = getRoomsByNames(roomNames);
        if (fromTime != null && toTime != null && isInvalidTimeRange(fromTime, toTime)) {
            return null;
        }
        if (!isRoomTypeValid(existing.getDefinition().getType(), rooms)) {
            return null;
        }
        if (!isRoomCapacityValid(expectedNumber, rooms, existing.getDefinition().getType())) {
            return null;
        }
        if (fromTime != null && toTime != null && hasRoomOverlap(existing.getId(), fromTime, toTime, rooms)) {
            return null;
        }

        existing.setSession(session);
        existing.setDurationMinutes(durationMinutes);
        existing.setPreviousYearAttendantsNumber(previousYearAttendantsNumber);
        existing.setPreviousYearTotalStudents(previousYearTotalStudents);
        existing.setAttendantsNumber(attendantsNumber);
        existing.setTotalStudents(totalStudents);
        existing.setExpectedNumber(expectedNumber);
        existing.setNumRepetitions(numRepetitions);
        existing.setFromTime(fromTime);
        existing.setToTime(toTime);
        existing.setTimeSlot(buildAndSaveTimeSlot(fromTime, toTime));
        existing.setRooms(rooms);
        existing.setComment(comment);
        return this.subjectExamRepository.save(existing);
    }

    @Override
    public SubjectExam delete(String name) {
        SubjectExam subjectExam = findByName(name);
        subjectExam.getRooms().clear();
        subjectExamRepository.delete(subjectExam);
        return subjectExam;
    }

    @Override
    public SubjectExam save(SubjectExam exam) {
        return subjectExamRepository.save(exam);
    }

    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Override
    public boolean updateSubjectExamTime(String id, String newFromTime, String newToTime) {
        return updateSubjectExamPlacement(id, newFromTime, newToTime, null);
    }

    @Override
    public boolean updateSubjectExamPlacement(String id, String newFromTime, String newToTime, String roomName) {
        try {
            SubjectExam examToUpdate = findByName(id);
            if (!canManageExam(examToUpdate)) {
                return false;
            }
            LocalDateTime fromTime = LocalDateTime.parse(newFromTime, INPUT_FMT);
            LocalDateTime toTime = LocalDateTime.parse(newToTime, INPUT_FMT);
            if (isInvalidTimeRange(fromTime, toTime)) {
                return false;
            }

            Set<Room> targetRooms;
            if (roomName != null && !roomName.isBlank()) {
                targetRooms = roomService.findAllByNameIn(Set.of(roomName));
                if (targetRooms.isEmpty()) {
                    return false;
                }
            } else {
                targetRooms = examToUpdate.getRooms() == null ? new HashSet<>() : new HashSet<>(examToUpdate.getRooms());
            }

            if (!isRoomTypeValid(examToUpdate.getDefinition().getType(), targetRooms)) {
                return false;
            }
            if (!isRoomCapacityValid(examToUpdate.getExpectedNumber(), targetRooms, examToUpdate.getDefinition().getType())) {
                return false;
            }
            if (hasRoomOverlap(examToUpdate.getId(), fromTime, toTime, targetRooms)) {
                throw new OverlappingExamTimesInTheSameRoomException(
                        "Exam time overlaps with another exam in the same room.");
            }

            examToUpdate.setFromTime(fromTime);
            examToUpdate.setToTime(toTime);
            examToUpdate.setRooms(targetRooms);
            examToUpdate.setTimeSlot(buildAndSaveTimeSlot(fromTime, toTime));
            this.subjectExamRepository.save(examToUpdate);
            return true;
        } catch (Exception e) {
            log.error("Unable to update exam time for {}", id, e);
            return false;
        }
    }

    @Override
    public boolean checkInvalidDateTimeInput(String fromTime, String toTime) {
        try {
            LocalDateTime from = LocalDateTime.parse(fromTime, INPUT_FMT);
            LocalDateTime to = LocalDateTime.parse(toTime, INPUT_FMT);
            return isInvalidTimeRange(from, to);
        } catch (Exception ex) {
            return true;
        }
    }

    @Override
    public void examCalculations(String yearExamSession) {
        YearExamSession session = this.yearExamSessionRepository.findById(yearExamSession).orElseThrow(InvalidYearExamSessionException::new);
        List<SubjectExam> exams = this.subjectExamRepository.findAllBySession(session);

        for (SubjectExam exam : exams) {
            getSubjectAbbreviation(exam);
            ExamType examType = exam.getDefinition().getType();
            if (examType.equals(ExamType.LAB) || examType.equals(ExamType.CLASSROOM)) {
                List<Room> rooms;
                if (examType.equals(ExamType.LAB)) {
                    rooms = roomService.findAllByRoomType(RoomType.LAB);
                } else {
                    rooms = roomService.findAllByRoomType(RoomType.CLASSROOM);
                }

                int totalCapacity = roomService.calculateTotalCapacityOfRooms(rooms);
                if (totalCapacity != 0) {
                    long numRepetitions = (long) Math.ceil((double) exam.getExpectedNumber() / totalCapacity);
                    exam.setNumRepetitions(numRepetitions);
                    if (numRepetitions > 1) {
                        exam.setRooms(new HashSet<>(rooms));
                    }
                } else {
                    exam.setNumRepetitions(0L);
                }
            } else {
                exam.setNumRepetitions(1L);
                // handle online and homework exams here
            }

            subjectExamRepository.save(exam);
        }
    }

    @Override
    public SubjectExam updateSubjectExamNumRepetitions(String id, Long numRepetitions) {
        SubjectExam exam = this.subjectExamRepository.findById(id).orElseThrow(SubjectExamNotFoundException::new);
        exam.setNumRepetitions(numRepetitions);
        return this.subjectExamRepository.save(exam);
    }

    @Override
    public SubjectExam recalculateSubjectExam(String id) {
        SubjectExam exam = this.subjectExamRepository.findById(id).orElseThrow(SubjectExamNotFoundException::new);

        getSubjectAbbreviation(exam);
        Set<Room> rooms = exam.getRooms();
        int totalCapacity = roomService.calculateTotalCapacityOfRooms(rooms.stream().toList());
        if (totalCapacity != 0) {
            long numRepetitions = (long) Math.ceil((double) exam.getExpectedNumber() / totalCapacity);
            exam.setNumRepetitions(numRepetitions);
            if (numRepetitions > 1) {
                exam.setRooms(rooms);
            }
        } else {
            exam.setNumRepetitions(0L);
        }

        return this.subjectExamRepository.save(exam);
    }

    private void getSubjectAbbreviation(SubjectExam exam) {
        String subject = exam.getId().split("-")[2];
        Optional<JoinedSubject> joinedSubject = joinedSubjectRepository.findByAbbreviation(subject);


        if (joinedSubject.isPresent() && subjectAllocationStatsService.findBySubject(joinedSubject.get()).isPresent()) {
            SubjectAllocationStats subjectAllocationStats = subjectAllocationStatsService.findBySubject(joinedSubject.get()).get();
            exam.setTotalStudents(Long.valueOf(subjectAllocationStatsService.getTotalStudents(subjectAllocationStats)));
        } else {
            if (exam.getPreviousYearTotalStudents() != null)
                exam.setTotalStudents(exam.getPreviousYearTotalStudents());
            else {
                exam.setTotalStudents(0L);
            }
        }

        long previousYearAttendantsNumber = (exam.getPreviousYearAttendantsNumber() != null) ? exam.getPreviousYearAttendantsNumber() : 0L;
        long previousYearTotalStudents = (exam.getPreviousYearTotalStudents() != null) ? exam.getPreviousYearTotalStudents() : 0L;

        if (previousYearAttendantsNumber > 0 && previousYearTotalStudents > 0) {
            long expectedNumber = (long) Math.ceil((1.05 * previousYearAttendantsNumber / previousYearTotalStudents) * exam.getTotalStudents());
            exam.setExpectedNumber(expectedNumber);
        } else {
            exam.setExpectedNumber(exam.getTotalStudents());
        }
    }

    private boolean areTimesOverlapping(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private boolean isInvalidTimeRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || !to.isAfter(from)) {
            return true;
        }
        long minutes = java.time.Duration.between(from, to).toMinutes();
        return minutes < 15 || minutes % 15 != 0;
    }

    private boolean isRoomTypeValid(ExamType type, Set<Room> rooms) {
        if (type != ExamType.LAB && type != ExamType.CLASSROOM) {
            return true;
        }
        RoomType expectedRoomType = type == ExamType.LAB ? RoomType.LAB : RoomType.CLASSROOM;
        return rooms.stream().allMatch(room -> room.getType() == expectedRoomType);
    }

    private boolean isRoomCapacityValid(Long expectedNumber, Set<Room> rooms, ExamType examType) {
        if (examType != ExamType.LAB && examType != ExamType.CLASSROOM) {
            return true;
        }
        long expected = Optional.ofNullable(expectedNumber).orElse(0L);
        long capacity = rooms.stream().mapToLong(room -> Optional.ofNullable(room.getCapacity()).orElse(0L)).sum();
        return capacity >= expected;
    }

    private boolean hasRoomOverlap(String currentExamId, LocalDateTime from, LocalDateTime to, Set<Room> rooms) {
        for (Room room : rooms) {
            List<SubjectExam> existingInRoom = subjectExamRepository.findByRoomsContaining(room);
            for (SubjectExam existing : existingInRoom) {
                if (existing.getId().equals(currentExamId) || existing.getFromTime() == null || existing.getToTime() == null) {
                    continue;
                }
                if (areTimesOverlapping(from, to, existing.getFromTime(), existing.getToTime())) {
                    return true;
                }
            }
        }
        return false;
    }

    private TimeSlot buildAndSaveTimeSlot(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return null;
        }
        // Ensure times are 15-minute aligned before saving
        long durationMinutes = java.time.Duration.between(from, to).toMinutes();
        if (durationMinutes < 15 || durationMinutes % 15 != 0) {
            // Adjust toTime to align with 15-minute boundary
            long alignedMinutes = Math.max(15, ((durationMinutes + 14) / 15) * 15);
            to = from.plusMinutes(alignedMinutes);
        }
        return timeSlotRepository.save(new TimeSlot(from, to));
    }

    private boolean canManageExam(SubjectExam exam) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean hasAdminRole = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (hasAdminRole || currentUserService.isAdmin()) {
            return true;
        }
        boolean hasProfessorRole = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_PROFESSOR".equals(a.getAuthority()));
        if (!hasProfessorRole && !currentUserService.isProfessor()) {
            return false;
        }
        String subjectId = exam.getDefinition().getSubject().getAbbreviation();
        String professorId = currentUserService.getCurrentUser().map(User::getId).orElse(null);
        if (professorId == null) {
            // Form-login users are not mapped to domain User; role check above already passed.
            return true;
        }
        return !teacherSubjectAllocationsRepository.findAllByProfessorIdAndSubjectId(professorId, subjectId).isEmpty();
    }

    @Override
    public void removeRoom(String seName, String roomName) {
        SubjectExam exam = findByName(seName);
        exam.getRooms().removeIf(r -> r.getName().equals(roomName));
        subjectExamRepository.save(exam);
    }

    @Override
    public SubjectExam CheckPreviousYear(String name, ExamDefinition definition, YearExamSession session) {
        Optional<SubjectExam> priorOpt = findPreviousYearExam(name);
        Optional<SubjectExam> currentOpt = subjectExamRepository.findById(name);
        SubjectExam exam = currentOpt.orElseGet(() -> new SubjectExam(definition, session));

        if (currentOpt.isEmpty()) {
            exam.setId(name);
            exam.setNumRepetitions(1L);
            exam.setDurationMinutes(definition.getDurationMinutes());
        }
        if (priorOpt.isPresent()) {
            SubjectExam prior = priorOpt.get();
            exam.setDurationMinutes(definition.getDurationMinutes());
            exam.setPreviousYearAttendantsNumber(prior.getAttendantsNumber());
            exam.setPreviousYearTotalStudents(prior.getTotalStudents());
            exam.setExpectedNumber(prior.getExpectedNumber());
            exam.setNumRepetitions(prior.getNumRepetitions() == null ? 1L : prior.getNumRepetitions());
            exam.setRooms(new HashSet<>(prior.getRooms()));
            exam.setComment(prior.getComment());
        }
        return exam;
    }

    private Optional<SubjectExam> findPreviousYearExam(String currentId) {
        String[] parts = currentId.split("-", 3);
        if (parts.length < 3) {
            return Optional.empty();
        }
        if (!parts[0].matches("\\d{4}") || !parts[1].matches("\\d{1,4}")) {
            // Some session names use format like 2026-FIRST_MIDTERM where prior-year ID cannot be derived.
            return Optional.empty();
        }

        try {
            int start = Integer.parseInt(parts[0]) - 1;
            int end = Integer.parseInt(parts[1]) - 1;
            String priorId = String.format("%d-%02d-%s", start, end, parts[2]);
            return subjectExamRepository.findByIdWithRooms(priorId);
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<Room> getRoomsByType(String name) {
        SubjectExam se = subjectExamRepository.findById(name).orElseThrow(SubjectExamNotFoundException::new);
        ExamType examType = se.getDefinition().getType();
        RoomType roomType;
        switch (examType) {
            case LAB:
                roomType = RoomType.LAB;
                break;
            case CLASSROOM:
                roomType = RoomType.CLASSROOM;
                break;
            default:
                return Collections.emptyList();
        }
        return roomService.findAllByRoomType(roomType);
    }

    @Override
    public Boolean needsRooms(String name) {
        SubjectExam se = subjectExamRepository.findById(name).orElseThrow(SubjectExamNotFoundException::new);
        ExamType type = se.getDefinition().getType();
        return type == ExamType.LAB || type == ExamType.CLASSROOM;
    }

    @Override
    public List<SubjectExam> findByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);

        Specification<SubjectExam> onDate = (root, query, cb) ->
                cb.between(root.get("fromTime"), start, end);

        return subjectExamRepository.findAll(onDate, PageRequest.of(0, 1000, Sort.by("fromTime"))).getContent();
    }

    @Override
    public Set<String> findOverlappingExamIds(List<SubjectExam> exams) {
        Set<String> overlaps = new HashSet<>();
        exams.stream()
                .filter(e -> e.getFromTime() != null && e.getToTime() != null)
                .flatMap(e -> e.getRooms().stream().map(room -> Map.entry(room.getName(), e)))
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())))
                .values()
                .forEach(list -> {
                    list.sort(Comparator.comparing(SubjectExam::getFromTime));
                    for (int i = 0; i < list.size(); i++) {
                        for (int j = i + 1; j < list.size(); j++) {
                            SubjectExam a = list.get(i);
                            SubjectExam b = list.get(j);
                            if (areTimesOverlapping(a.getFromTime(), a.getToTime(), b.getFromTime(), b.getToTime())) {
                                overlaps.add(a.getId());
                                overlaps.add(b.getId());
                            }
                        }
                    }
                });
        return overlaps;
    }

    @Override
    public boolean submitExpectedStudents(String subjectExamId, Long expectedStudents) {
        SubjectExam exam = findByName(subjectExamId);
        if (!canManageExam(exam) || expectedStudents == null || expectedStudents <= 0) {
            return false;
        }

        LocalDateTime examReference = exam.getFromTime() != null
                ? exam.getFromTime()
                : exam.getSession().getSessionStart().atTime(8, 0);
        if (LocalDateTime.now().isAfter(examReference.minusWeeks(3))) {
            return false;
        }

        exam.setExpectedNumber(expectedStudents);
        exam.setExpectedStudentsSubmittedAt(LocalDateTime.now());
        exam.setWorkflowStatus(ExamWorkflowStatus.SUBMITTED);
        subjectExamRepository.save(exam);
        return true;
    }

    @Override
    public void markSessionStatus(String yearExamSessionName, ExamWorkflowStatus status) {
        YearExamSession session = yearExamSessionRepository.findById(yearExamSessionName)
                .orElseThrow(InvalidYearExamSessionException::new);
        List<SubjectExam> exams = subjectExamRepository.findAllBySession(session);
        exams.forEach(exam -> exam.setWorkflowStatus(status));
        subjectExamRepository.saveAll(exams);
    }
}