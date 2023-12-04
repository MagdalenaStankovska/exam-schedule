package mk.ukim.finki.exam_schedule.model.dto;

import lombok.Data;
import mk.ukim.finki.exam_schedule.model.Course;
import mk.ukim.finki.exam_schedule.model.YearExamSession;

@Data
public class CourseExamPartDto {

    private Course course;

    private YearExamSession session;

    private String name;


    public CourseExamPartDto(Course course, YearExamSession session, String name) {
        this.course = course;
        this.session = session;
        this.name = name;
    }
}
