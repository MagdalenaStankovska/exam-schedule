package mk.ukim.finki.exam_schedule.repository;

import mk.ukim.finki.exam_schedule.model.Semester;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SemesterRepository extends JpaRepository<Semester, String> {
    @Override
    Optional<Semester> findById(String s);
}
