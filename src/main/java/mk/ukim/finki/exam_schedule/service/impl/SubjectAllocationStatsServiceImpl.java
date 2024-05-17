package mk.ukim.finki.exam_schedule.service.impl;

import mk.ukim.finki.exam_schedule.model.SubjectAllocationStats;
import mk.ukim.finki.exam_schedule.repository.SubjectAllocationStatsRepository;
import mk.ukim.finki.exam_schedule.service.SubjectAllocationStatsService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SubjectAllocationStatsServiceImpl implements SubjectAllocationStatsService {

    private final SubjectAllocationStatsRepository subjectAllocationStatsRepository;

    public SubjectAllocationStatsServiceImpl(SubjectAllocationStatsRepository subjectAllocationStatsRepository) {
        this.subjectAllocationStatsRepository = subjectAllocationStatsRepository;
    }

    @Override
    public Optional<SubjectAllocationStats> findBySemesterAndSubject(String semester, String subject) {
        return this.subjectAllocationStatsRepository.findById(SubjectAllocationStats.constructId(semester, subject));
    }

    @Override
    public Optional<SubjectAllocationStats> findById(String id) {
        return subjectAllocationStatsRepository.findById(id);
    }

    @Override
    public Integer getTotalStudents(SubjectAllocationStats subjectAllocationStats) {
        return subjectAllocationStats.getTotalStudents();
    }
}
