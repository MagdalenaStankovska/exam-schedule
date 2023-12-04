package mk.ukim.finki.exam_schedule.repository;

import mk.ukim.finki.exam_schedule.model.YearExamSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public interface YearExamSessionRepository extends JpaSpecificationRepository<YearExamSession, String> {

    Page<YearExamSession> findAll(Specification<YearExamSession> filter, Pageable page);
}
