package mk.ukim.finki.exam_schedule.service;

import mk.ukim.finki.exam_schedule.model.ExamDefinition;
import mk.ukim.finki.exam_schedule.model.Room;
import mk.ukim.finki.exam_schedule.model.SubjectExam;
import mk.ukim.finki.exam_schedule.model.YearExamSession;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Set;

public interface SubjectExamService {
    SubjectExam findByName(String name);
    Page<SubjectExam> findAll(Specification<SubjectExam> filter, Integer page, Integer size);
    SubjectExam create(YearExamSession session, ExamDefinition definition);
    SubjectExam update(String name, YearExamSession session,
                       Long durationMinutes, Long previousYearAttendantsNumber, Long previousYearTotalStudents,
                       Long attendantsNumber, Long totalStudents, Long expectedNumber,
                       Long numRepetitions, LocalDateTime fromTime, LocalDateTime toTime,
                       Set<String> roomNames, String comment);
    Set<Room> getAllRooms();
    Set<Room> getRoomsByNames(Set<String> roomNames);
    SubjectExam delete(String id);

    SubjectExam save(SubjectExam exam);

    boolean updateSubjectExamTime (String id, String newFromTime, String newToTime);

    void examCalculations (String yearExamSession);
}
