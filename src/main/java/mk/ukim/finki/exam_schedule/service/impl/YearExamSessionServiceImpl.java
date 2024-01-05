package mk.ukim.finki.exam_schedule.service.impl;

import mk.ukim.finki.exam_schedule.model.*;
import mk.ukim.finki.exam_schedule.model.exceptions.InvalidYearExamSessionException;
import mk.ukim.finki.exam_schedule.repository.CourseRepository;
import mk.ukim.finki.exam_schedule.repository.YearExamSessionRepository;
import mk.ukim.finki.exam_schedule.repository.SemesterRepository;
import mk.ukim.finki.exam_schedule.service.YearExamSessionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.data.domain.Sort.by;

@Service
public class YearExamSessionServiceImpl implements YearExamSessionService {

    private final YearExamSessionRepository yearExamSessionRepository;
    private final SemesterRepository semesterRepository;
    private final CourseRepository courseRepository;

    public YearExamSessionServiceImpl(YearExamSessionRepository yearExamSessionRepository, SemesterRepository semesterRepository, CourseRepository courseRepository){

        this.yearExamSessionRepository = yearExamSessionRepository;
        this.semesterRepository = semesterRepository;
        this.courseRepository = courseRepository;
    }


    @Override
    public YearExamSession findByName(String name) {
        return this.yearExamSessionRepository.findById(name).orElseThrow(InvalidYearExamSessionException::new);
    }

    @Override
    public Page<YearExamSession> findAll(Specification<YearExamSession> filter, Integer page, Integer size) {
        return this.yearExamSessionRepository.findAll(filter, PageRequest.of(page - 1, size,
                by("year")
                    .and(by(ASC, "sessionStart"))));
    }

    @Override
    public YearExamSession create(String name, ExamSession session, String year, LocalDate sessionStart, LocalDate sessionEnd, LocalDate enrollmentStartDate, LocalDate enrollmentEndDate, List<StudyCycle> cycle) {
        return this.yearExamSessionRepository.save(new YearExamSession(
                name,
                session,
                year,
                sessionStart,
                sessionEnd,
                enrollmentStartDate,
                enrollmentEndDate,
                cycle));
    }


    @Override
    public YearExamSession update(String name, ExamSession session, String year, LocalDate sessionStart, LocalDate sessionEnd, LocalDate enrollmentStartDate, LocalDate enrollmentEndDate, List<StudyCycle> cycle) {
        YearExamSession ses = this.findByName(name);
        ses.setSession(session);
        ses.setYear(year);
        ses.setSessionStart(sessionStart);
        ses.setSessionEnd(sessionEnd);
        ses.setEnrollmentStartDate(enrollmentStartDate);
        ses.setEnrollmentEndDate(enrollmentEndDate);
        ses.setCycle(cycle);
        return this.yearExamSessionRepository.save(ses);
    }

    @Override
    public YearExamSession delete(String name) {
        YearExamSession ses = findByName(name);
        this.yearExamSessionRepository.delete(ses);
        return ses;
    }

    @Override
    public List<Course> findAllCourses(String year) {
        Semester semester1 = new Semester(year, "W");
        Semester semester2 = new Semester(year, "S");
        List<Course> coursesSemester1 = courseRepository.findAllBySemester(semester1);
        List<Course> coursesSemester2 = courseRepository.findAllBySemester(semester2);

        List<Course> combinedCourses = new ArrayList<>(coursesSemester1);
        combinedCourses.addAll(coursesSemester2);

        return combinedCourses;
    }

}
