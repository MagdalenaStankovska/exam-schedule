package mk.ukim.finki.exam_schedule.service;

import mk.ukim.finki.exam_schedule.model.Course;
import mk.ukim.finki.exam_schedule.model.CourseExamPart;
import mk.ukim.finki.exam_schedule.model.YearExamSession;
import mk.ukim.finki.exam_schedule.model.dto.CourseExamPartDto;

import java.util.List;
import java.util.Optional;

public interface CourseExamPartService{

    List <CourseExamPart> listAll();
    Optional<CourseExamPart> findById(String id);
    Optional<CourseExamPart> save(Course course, YearExamSession session, String name);
    Optional<CourseExamPart> save(CourseExamPartDto courseExamPartDto);
    Optional<CourseExamPart> edit(String id, Course course, YearExamSession session, String name);
    Optional<CourseExamPart> edit(String id, CourseExamPartDto courseExamPartDto);
    Optional<CourseExamPart> delete(String id);

}
