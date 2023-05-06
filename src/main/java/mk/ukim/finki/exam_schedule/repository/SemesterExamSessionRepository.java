package mk.ukim.finki.exam_schedule.repository;

import mk.ukim.finki.exam_schedule.model.Semester;
import mk.ukim.finki.exam_schedule.model.SemesterExamSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SemesterExamSessionRepository extends JpaRepository<SemesterExamSession,String> {
    List<SemesterExamSession> findBySemester(Semester semester);
}
