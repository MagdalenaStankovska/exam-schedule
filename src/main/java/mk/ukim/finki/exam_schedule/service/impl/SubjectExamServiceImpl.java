package mk.ukim.finki.exam_schedule.service.impl;

import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.exam_schedule.model.*;
import mk.ukim.finki.exam_schedule.model.exceptions.OverlappingExamTimesInTheSameRoomException;
import mk.ukim.finki.exam_schedule.model.exceptions.SubjectExamNotFoundException;
import mk.ukim.finki.exam_schedule.repository.RoomRepository;
import mk.ukim.finki.exam_schedule.repository.SubjectAllocationStatsRepository;
import mk.ukim.finki.exam_schedule.repository.SubjectExamRepository;
import mk.ukim.finki.exam_schedule.service.RoomService;
import mk.ukim.finki.exam_schedule.service.SubjectAllocationStatsService;
import mk.ukim.finki.exam_schedule.service.SubjectExamService;
import mk.ukim.finki.exam_schedule.service.YearExamSessionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class SubjectExamServiceImpl implements SubjectExamService {

    private final SubjectExamRepository subjectExamRepository;
    private final YearExamSessionService yearExamSessionService;
    private final RoomService roomService;
    private final SubjectAllocationStatsService subjectAllocationStatsService;

    public SubjectExamServiceImpl(SubjectExamRepository subjectExamRepository, YearExamSessionService yearExamSessionService, RoomService roomService, SubjectAllocationStatsService subjectAllocationStatsService) {
        this.subjectExamRepository = subjectExamRepository;
        this.yearExamSessionService = yearExamSessionService;
        this.roomService = roomService;
        this.subjectAllocationStatsService = subjectAllocationStatsService;
    }

    @Override
    public SubjectExam findByName(String name) {
        return this.subjectExamRepository.findById(name).orElseThrow(SubjectExamNotFoundException::new);
    }

    @Override
    public Page<SubjectExam> findAll(Specification<SubjectExam> filter, Integer page, Integer size) {
        return this.subjectExamRepository.findAll(filter, PageRequest.of(page - 1, size));
    }

    @Override
    public SubjectExam create(YearExamSession session, ExamDefinition definition) {
        return this.subjectExamRepository.save(new SubjectExam(definition, session));
    }

    @Override
    public Set<Room> getAllRooms() {
        return new HashSet<>(roomService.findAll());
    }

    @Override
    public Set<Room> getRoomsByNames(Set<String> roomNames) {
        return roomService.findAllByNameIn(roomNames);
    }

    @Override
    public SubjectExam update(String name, YearExamSession session, Long durationMinutes,
                              Long previousYearAttendantsNumber, Long previousYearTotalStudents,
                              Long attendantsNumber, Long totalStudents, Long expectedNumber,
                              Long numRepetitions, LocalDateTime fromTime, LocalDateTime toTime,
                              Set<String> roomNames, String comment) {
        Set<Room> rooms = getRoomsByNames(roomNames);
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
        subjectExamRepository.delete(subjectExam);
        return subjectExam;
    }

    @Override
    public SubjectExam save(SubjectExam exam) {
        return subjectExamRepository.save(exam);
    }

    @Override
    public boolean updateSubjectExamTime(String id, String newFromTime, String newToTime) {
        try {
            SubjectExam examToUpdate = findByName(id);
            LocalDateTime fromTime = LocalDateTime.parse(newFromTime);
            LocalDateTime toTime = LocalDateTime.parse(newToTime);
            Set<SubjectExam> examsInTheSameRoom = new HashSet<>();
            for (Room room : examToUpdate.getRooms()) {
                examsInTheSameRoom.addAll(subjectExamRepository.findByRoomsContaining(room));
            }

            for (SubjectExam exam : examsInTheSameRoom) {
                if (!exam.getId().equals(examToUpdate.getId()) && areTimesOverlapping(fromTime, toTime, exam.getFromTime(), exam.getToTime())) {
                    log.info("Overlapping Exam Times - Exception thrown");
                    throw new OverlappingExamTimesInTheSameRoomException("Exam time overlaps with another exam in the same room.");
                }
            }
            examToUpdate.setFromTime(fromTime);
            examToUpdate.setToTime(toTime);
            this.subjectExamRepository.save(examToUpdate);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    @Override
    public void examCalculations(String yearExamSession) {
        YearExamSession session = this.yearExamSessionService.findByName(yearExamSession);
        List<SubjectExam> exams = this.subjectExamRepository.findAllBySession(session);

        for (SubjectExam exam : exams) {
            String[] parts = exam.getId().split("-");
            String[] subArray = Arrays.copyOfRange(parts, 1, 5);
            String statsId = String.join("-", subArray);

            if(subjectAllocationStatsService.findById(statsId).isPresent()) {
                SubjectAllocationStats subjectAllocationStats = subjectAllocationStatsService.findById(statsId).get();
                exam.setTotalStudents(Long.valueOf(subjectAllocationStatsService.getTotalStudents(subjectAllocationStats)));
            }
            else {
                if(exam.getPreviousYearTotalStudents()!=null) exam.setTotalStudents(Long.valueOf(exam.getPreviousYearTotalStudents()));
                else{
                    exam.setTotalStudents(Long.valueOf(0));
                }
            }

            Long previousYearAttendantsNumber = exam.getPreviousYearAttendantsNumber();
            Long previousYearTotalStudents = exam.getPreviousYearTotalStudents();

            if(previousYearAttendantsNumber > 0 && previousYearTotalStudents > 0){
                long expectedNumber = (long) Math.ceil((1.05 * previousYearAttendantsNumber / previousYearTotalStudents) * exam.getTotalStudents());
                exam.setExpectedNumber(expectedNumber);
            }
            else{
                exam.setExpectedNumber(exam.getTotalStudents());
            }
            ExamType examType = exam.getDefinition().getType();
            if(examType.equals(ExamType.LAB) || examType.equals(ExamType.CLASSROOM)){
                List<Room> rooms = new ArrayList<>();
                if(examType.equals(ExamType.LAB)) {
                    rooms = roomService.findAllByRoomType(RoomType.LAB);
                }
                else {
                    rooms = roomService.findAllByRoomType(RoomType.CLASSROOM);
                }

                int totalCapacity = roomService.calculateTotalCapacityOfRooms(rooms);
                if (totalCapacity != 0) {
                    long numRepetitions = (long) Math.ceil((double) exam.getExpectedNumber() / totalCapacity);
                    exam.setNumRepetitions(numRepetitions);
                    if (numRepetitions > 1){
                        exam.setRooms((Set<Room>) rooms);
                    }
                }
                else {
                    exam.setNumRepetitions(Long.valueOf(0));
                }
            }
            else {
                exam.setNumRepetitions(Long.valueOf(1));
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

        String[] parts = exam.getId().split("-");
        String[] subArray = Arrays.copyOfRange(parts, 1, 5);
        String statsId = String.join("-", subArray);

        if(subjectAllocationStatsService.findById(statsId).isPresent()) {
            SubjectAllocationStats subjectAllocationStats = subjectAllocationStatsService.findById(statsId).get();
            exam.setTotalStudents(Long.valueOf(subjectAllocationStatsService.getTotalStudents(subjectAllocationStats)));
        }
        else {
            if(exam.getPreviousYearTotalStudents()!=null) exam.setTotalStudents(Long.valueOf(exam.getPreviousYearTotalStudents()));
            else{
                exam.setTotalStudents(Long.valueOf(0));
            }
        }

        Long previousYearAttendantsNumber = exam.getPreviousYearAttendantsNumber();
        Long previousYearTotalStudents = exam.getPreviousYearTotalStudents();

        if(previousYearAttendantsNumber > 0 && previousYearTotalStudents > 0){
            long expectedNumber = (long) Math.ceil((1.05 * previousYearAttendantsNumber / previousYearTotalStudents) * exam.getTotalStudents());
            exam.setExpectedNumber(expectedNumber);
        }
        else{
            exam.setExpectedNumber(exam.getTotalStudents());
        }
        Set<Room> rooms = exam.getRooms();
        int totalCapacity = roomService.calculateTotalCapacityOfRooms(rooms.stream().toList());
        if (totalCapacity != 0) {
            long numRepetitions = (long) Math.ceil((double) exam.getExpectedNumber() / totalCapacity);
            exam.setNumRepetitions(numRepetitions);
            if (numRepetitions > 1){
                exam.setRooms((Set<Room>) rooms);
            }
        }
        else {
            exam.setNumRepetitions(Long.valueOf(0));
        }

        return this.subjectExamRepository.save(exam);
    }

    private boolean areTimesOverlapping(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }


}