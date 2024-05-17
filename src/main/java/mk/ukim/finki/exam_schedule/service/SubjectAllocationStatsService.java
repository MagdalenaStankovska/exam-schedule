package mk.ukim.finki.exam_schedule.service;

import mk.ukim.finki.exam_schedule.model.SubjectAllocationStats;

import java.util.Optional;

public interface SubjectAllocationStatsService {

    Optional<SubjectAllocationStats> findBySemesterAndSubject(String semester, String subject);

    Optional<SubjectAllocationStats> findById(String id);

    public Integer getTotalStudents(SubjectAllocationStats subjectAllocationStats);
}
