package mk.ukim.finki.exam_schedule.repository;

import mk.ukim.finki.exam_schedule.model.TeacherSubjectAllocations;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface TeacherSubjectAllocationsRepository extends JpaRepository<TeacherSubjectAllocations, Long> {
    List<TeacherSubjectAllocations> findAllByProfessorId(String professorId);

    List<TeacherSubjectAllocations> findAllByProfessorIdAndSubjectId(String professorId, String subjectId);

    List<TeacherSubjectAllocations> findAllBySubjectIdIn(Set<String> subjectIds);
}

