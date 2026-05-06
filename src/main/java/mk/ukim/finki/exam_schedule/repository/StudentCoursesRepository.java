package mk.ukim.finki.exam_schedule.repository;

import mk.ukim.finki.exam_schedule.model.StudentCourses;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface StudentCoursesRepository extends JpaRepository<StudentCourses, Long> {
    List<StudentCourses> findAllByCourse_JoinedSubject_AbbreviationIn(Set<String> subjectAbbreviations);
}

