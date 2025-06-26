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
import mk.ukim.finki.exam_schedule.repository.YearExamSessionRepository;
import mk.ukim.finki.exam_schedule.service.RoomService;
import mk.ukim.finki.exam_schedule.service.SubjectAllocationStatsService;
import mk.ukim.finki.exam_schedule.service.SubjectExamService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    private final RoomService roomService;

    public SubjectExamServiceImpl(SubjectExamRepository subjectExamRepository, YearExamSessionRepository yearExamSessionRepository, ExamDefinitionRepository examDefinitionRepository, JoinedSubjectRepository joinedSubjectRepository, SubjectAllocationStatsService subjectAllocationStatsService, RoomService roomService) {
        this.subjectExamRepository = subjectExamRepository;
        this.yearExamSessionRepository = yearExamSessionRepository;
        this.examDefinitionRepository = examDefinitionRepository;
        this.joinedSubjectRepository = joinedSubjectRepository;
        this.subjectAllocationStatsService = subjectAllocationStatsService;
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
        Set<Room> rooms = getRoomsByNames(roomNames);
        if (fromTime != null && toTime != null && checkInvalidDateTimeInput(fromTime.toString(), toTime.toString())) {
            return null;
        }
        SubjectExam subjectExam = this.findByName(name);
        subjectExam.setSession(session);
        subjectExam.setDurationMinutes(durationMinutes);
        subjectExam.setPreviousYearAttendantsNumber(previousYearAttendantsNumber);
        subjectExam.setPreviousYearTotalStudents(previousYearTotalStudents);
        subjectExam.setAttendantsNumber(attendantsNumber);
        subjectExam.setTotalStudents(totalStudents);
        subjectExam.setExpectedNumber(expectedNumber);
        subjectExam.setNumRepetitions(numRepetitions);
        subjectExam.setFromTime(fromTime);
        subjectExam.setToTime(toTime);
        subjectExam.setRooms(rooms);
        subjectExam.setComment(comment);
        return this.subjectExamRepository.save(subjectExam);
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
        try {
            SubjectExam examToUpdate = findByName(id);
            LocalDateTime fromTime = LocalDateTime.parse(newFromTime, INPUT_FMT);
            LocalDateTime toTime = LocalDateTime.parse(newToTime, INPUT_FMT);
            Set<SubjectExam> examsInTheSameRoom = examToUpdate.getRooms().stream()
                    .flatMap(r -> subjectExamRepository.findByRoomsContaining(r).stream())
                    .collect(Collectors.toSet());

            for (SubjectExam exam : examsInTheSameRoom) {
                if (exam.getId().equals(examToUpdate.getId())) continue;

                if (exam.getFromTime() == null || exam.getToTime() == null) continue;

                if (areTimesOverlapping(fromTime, toTime, exam.getFromTime(), exam.getToTime())) {
                    throw new OverlappingExamTimesInTheSameRoomException(
                            "Exam time overlaps with another exam in the same room.");
                }
            }
            examToUpdate.setFromTime(fromTime);
            examToUpdate.setToTime(toTime);
            this.subjectExamRepository.save(examToUpdate);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean checkInvalidDateTimeInput(String fromTime, String toTime) {
        LocalDateTime from = LocalDateTime.parse(fromTime, INPUT_FMT);
        LocalDateTime to = LocalDateTime.parse(toTime, INPUT_FMT);
        return from.isAfter(to) || from.isEqual(to);
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
                        exam.setRooms((Set<Room>) rooms);
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

    @Override
    public void removeRoom(String seName, String roomName) {
        SubjectExam exam = findByName(seName);
        exam.getRooms().removeIf(r -> r.getName().equals(roomName));
        subjectExamRepository.save(exam);
    }

    @Override
    public SubjectExam CheckPreviousYear(String name, ExamDefinition definition, YearExamSession session) {
        String[] parts = name.split("-", 3);
        int start = Integer.parseInt(parts[0]) - 1;
        int end = Integer.parseInt(parts[1]) - 1;
        String priorId = String.format("%d-%02d-%s", start, end, parts[2]);

        Optional<SubjectExam> priorOpt = subjectExamRepository.findByIdWithRooms(priorId);
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
        class Slot { String id, room; int start, end; }

        List<Slot> slots = new ArrayList<>();
        for (SubjectExam e : exams) {
            int sh = e.getFromTime().getHour();
            int durMin = Optional.ofNullable(e.getDurationMinutes()).orElse(0L).intValue();
            int eh = sh + (int) Math.ceil(durMin/60.0);
            for (Room r : e.getRooms()) {
                Slot s = new Slot();
                s.id = e.getId(); s.room = r.getName();
                s.start = sh; s.end = eh;
                slots.add(s);
            }
        }

        Set<String> overlaps = new HashSet<>();
        slots.stream()
                .collect(Collectors.groupingBy(s -> s.room))
                .values()
                .forEach(list -> {
                    list.sort(Comparator.comparingInt(s -> s.start));
                    for (int i = 0; i < list.size(); i++) {
                        for (int j = i+1; j < list.size(); j++) {
                            Slot a = list.get(i), b = list.get(j);
                            if (b.start < a.end) {
                                overlaps.add(a.id);
                                overlaps.add(b.id);
                            }
                        }
                    }
                });
        return overlaps;
    }
}