package mk.ukim.finki.exam_schedule.service;

import mk.ukim.finki.exam_schedule.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface SubjectExamService {
    SubjectExam findByName(String name);

    Page<SubjectExam> findAll(Specification<SubjectExam> filter, Integer page, Integer size);

    List<SubjectExam> findAllByExamDefinitionAndExamSession(ExamDefinition examDefinition, ExamSession examSession);

    SubjectExam create(YearExamSession session, ExamDefinition definition);

    void initialize(String yes);

    SubjectExam update(String name, YearExamSession session,
                       Long durationMinutes, Long previousYearAttendantsNumber, Long previousYearTotalStudents,
                       Long attendantsNumber, Long totalStudents, Long expectedNumber,
                       Long numRepetitions, LocalDateTime fromTime, LocalDateTime toTime,
                       Set<String> roomNames, String comment);

    Set<Room> getAllRooms();

    Set<Room> getRoomsByNames(Set<String> roomNames);

    SubjectExam delete(String id);

    SubjectExam save(SubjectExam exam);

    boolean updateSubjectExamTime(String id, String newFromTime, String newToTime);

    boolean updateSubjectExamPlacement(String id, String newFromTime, String newToTime, String roomName);

    void examCalculations(String yearExamSession);

    SubjectExam updateSubjectExamNumRepetitions(String id, Long numRepetitions);

    SubjectExam recalculateSubjectExam(String id);

    boolean checkInvalidDateTimeInput(String fromTime, String toTime);

    void removeRoom (String SeName, String roomName);

    SubjectExam CheckPreviousYear(String name, ExamDefinition definition, YearExamSession session);

    List<Room> getRoomsByType(String name);

    Boolean needsRooms(String name);

    List<SubjectExam> findByDate(LocalDate date);

    Set<String> findOverlappingExamIds(List<SubjectExam> exams);

    boolean submitExpectedStudents(String subjectExamId, Long expectedStudents);

    void markSessionStatus(String yearExamSessionName, ExamWorkflowStatus status);
}
