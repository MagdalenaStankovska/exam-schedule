package mk.ukim.finki.exam_schedule.model.dto;

import lombok.Data;
import mk.ukim.finki.exam_schedule.model.Course;
import mk.ukim.finki.exam_schedule.model.SemesterExamSession;

@Data
public class CourseExamPartDto {

    private Course course;

    private SemesterExamSession session;

    private String name;


    public CourseExamPartDto(Course course, SemesterExamSession session, String name) {
        this.course = course;
        this.session = session;
        this.name = name;
    }
}
