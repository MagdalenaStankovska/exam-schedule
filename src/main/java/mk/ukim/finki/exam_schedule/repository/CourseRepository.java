package mk.ukim.finki.exam_schedule.repository;

import mk.ukim.finki.exam_schedule.model.Course;
import mk.ukim.finki.exam_schedule.model.Semester;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, String> {

    List<Course> findAllBySemester(Semester semester);

}
