package mk.ukim.finki.exam_schedule.service.impl;

import javassist.NotFoundException;
import mk.ukim.finki.exam_schedule.model.ExamDefinition;
import mk.ukim.finki.exam_schedule.model.ExamSession;
import mk.ukim.finki.exam_schedule.model.ExamType;
import mk.ukim.finki.exam_schedule.model.JoinedSubject;
import mk.ukim.finki.exam_schedule.repository.ExamDefinitionRepository;
import mk.ukim.finki.exam_schedule.repository.JoinedSubjectRepository;
import mk.ukim.finki.exam_schedule.service.ExamDefinitionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ExamDefinitionServiceImpl implements ExamDefinitionService {

    private final ExamDefinitionRepository examDefinitionRepository;
    private final JoinedSubjectRepository joinedSubjectRepository;

    public ExamDefinitionServiceImpl(ExamDefinitionRepository examDefinitionRepository, JoinedSubjectRepository joinedSubjectRepository) {
        this.examDefinitionRepository = examDefinitionRepository;
        this.joinedSubjectRepository = joinedSubjectRepository;
    }

    @Override
    public List<ExamDefinition> findAll() {
        return this.examDefinitionRepository.findAll();
    }

    @Override
    public Page<ExamDefinition> findAllPaged(int page, int size, Specification<ExamDefinition> filter) {
        return examDefinitionRepository.findAll(filter, PageRequest.of(page - 1, size));
    }

    @Override
    public ExamDefinition findById(String id) {
        return examDefinitionRepository.findById(id).orElseThrow(NoSuchElementException::new);
    }

    @Override
    public void save(String subjectAbbreviation, Long durationMinutes, String type, String note) {
        JoinedSubject subject = this.joinedSubjectRepository.findByAbbreviation(subjectAbbreviation).orElseThrow(NoSuchElementException::new);
        Arrays.stream(ExamSession.values()).forEach(examSession -> {
                    String id = String.format("%s-%s-%s", subject.getAbbreviation(), examSession.toString(), type);
                    if (this.examDefinitionRepository.findById(id).isEmpty()) {
                        this.examDefinitionRepository.save(new ExamDefinition(id,
                                subject,
                                examSession,
                                durationMinutes,
                                ExamType.valueOf(type),
                                note
                        ));
                    }
                }
        );

    }

    @Override
    public void edit(String id, String subjectAbbreviation, Long durationMinutes, String type, String note) throws NotFoundException {
        JoinedSubject subject = this.joinedSubjectRepository.findByAbbreviation(subjectAbbreviation).orElseThrow(NoSuchElementException::new);
        ExamDefinition examDefinition = findById(id);
        examDefinition.setDurationMinutes(durationMinutes);
        examDefinition.setSubject(subject);
        examDefinition.setType(ExamType.valueOf(type));
        examDefinition.setNote(note);
        examDefinitionRepository.save(examDefinition);
    }

    @Override
    public Boolean deleteById(String id) {
        this.examDefinitionRepository.deleteById(id);
        return this.examDefinitionRepository.findById(id).isEmpty();
    }
}
