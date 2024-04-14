package mk.ukim.finki.exam_schedule.repository;

import mk.ukim.finki.exam_schedule.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface SubjectExamRepository extends JpaRepository<SubjectExam, String> {

    @Override
    Optional<SubjectExam> findById(String s);

    Page<SubjectExam> findAll(Specification<SubjectExam> filter, Pageable page);

    List<SubjectExam> findAllBySessionContains(YearExamSession session);

    List<SubjectExam> findByDefinition_Subject(JoinedSubject subject);

    List<SubjectExam> findBySessionCycle(StudyCycle studyCycle);

    List<SubjectExam> findByRoomsContaining(Room room);

}
