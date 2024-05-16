package mk.ukim.finki.exam_schedule.service;

import javassist.NotFoundException;
import mk.ukim.finki.exam_schedule.model.ExamDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface ExamDefinitionService {

    List<ExamDefinition> findAll();

    Page<ExamDefinition> findAllPaged(int page, int size, Specification<ExamDefinition> filter);

    ExamDefinition findById(String id);

    void save(String subjectAbbreviation, Long durationMinutes, String type, String note) throws NotFoundException;

    void edit(String id, String subjectAbbreviation, Long durationMinutes, String type, String note) throws NotFoundException;

    Boolean deleteById(String id);
}
