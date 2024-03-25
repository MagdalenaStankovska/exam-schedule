package mk.ukim.finki.exam_schedule.repository;

import mk.ukim.finki.exam_schedule.model.JoinedSubject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

public interface JoinedSubjectRepository extends JpaSpecificationRepository<JoinedSubject, String> {

    Optional<JoinedSubject> findByAbbreviation(String abbreviation);

    Page<JoinedSubject> findAll(Specification<JoinedSubject> filter, Pageable pageable);
}
