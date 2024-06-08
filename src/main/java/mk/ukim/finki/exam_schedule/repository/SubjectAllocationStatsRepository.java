package mk.ukim.finki.exam_schedule.repository;

import mk.ukim.finki.exam_schedule.model.SubjectAllocationStats;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectAllocationStatsRepository extends JpaSpecificationRepository<SubjectAllocationStats, String> {

}

