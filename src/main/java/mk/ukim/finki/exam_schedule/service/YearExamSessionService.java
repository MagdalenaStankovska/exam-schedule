package mk.ukim.finki.exam_schedule.service;

import mk.ukim.finki.exam_schedule.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;


public interface YearExamSessionService {

    Page<YearExamSession> findAll(Specification<YearExamSession> filter, Integer page, Integer size);
    List<YearExamSession> listAll();
    YearExamSession findByName(String name);
    YearExamSession create(ExamSession session, String year, LocalDate sessionStart, LocalDate sessionEnd, LocalDate enrollmentStartDate, LocalDate enrollmentEndDate, List<StudyCycle> cycle);
    YearExamSession update(String name, ExamSession session, String year, LocalDate sessionStart, LocalDate sessionEnd, LocalDate enrollmentStartDate, LocalDate enrollmentEndDate, List<StudyCycle> cycle);
    YearExamSession delete(String name);
    YearExamSession save(YearExamSession session);
    List<Course> findAllCourses(String year);

}
