package mk.ukim.finki.exam_schedule.repository;

import mk.ukim.finki.exam_schedule.model.JoinedSubject;
import mk.ukim.finki.exam_schedule.model.SubjectAllocationStats;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectAllocationStatsRepository extends JpaSpecificationRepository<SubjectAllocationStats, String> {


    List<SubjectAllocationStats> findAllBySubject(JoinedSubject subject);
}

