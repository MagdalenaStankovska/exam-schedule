package mk.ukim.finki.exam_schedule.service;

import mk.ukim.finki.exam_schedule.model.*;
import mk.ukim.finki.exam_schedule.repository.ExamDefinitionRepository;
import mk.ukim.finki.exam_schedule.repository.JoinedSubjectRepository;
import mk.ukim.finki.exam_schedule.service.impl.ExamDefinitionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamDefinitionServiceImplTest {

    @Mock
    private ExamDefinitionRepository examDefinitionRepository;
    @Mock
    private JoinedSubjectRepository joinedSubjectRepository;
    @Mock
    private SubjectExamService subjectExamService;

    private ExamDefinitionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ExamDefinitionServiceImpl(examDefinitionRepository, joinedSubjectRepository, subjectExamService);
    }

    @Test
    void findAllReturnsRepositoryContent() {
        ExamDefinition definition = new ExamDefinition();
        definition.setId("ALG-JUNE-LAB");
        when(examDefinitionRepository.findAll()).thenReturn(List.of(definition));

        assertThat(service.findAll()).containsExactly(definition);
    }

    @Test
    void findAllPagedDelegatesToRepository() {
        Page<ExamDefinition> page = new PageImpl<>(List.of(new ExamDefinition()));
        when(examDefinitionRepository.findAll(org.mockito.ArgumentMatchers.<Specification<ExamDefinition>>any(), any())).thenReturn(page);

        assertThat(service.findAllPaged(1, 10, Specification.where(null))).isEqualTo(page);
    }

    @Test
    void findByIdReturnsDefinitionOrThrowsWhenMissing() {
        ExamDefinition definition = new ExamDefinition();
        definition.setId("ALG-JUNE-LAB");
        when(examDefinitionRepository.findById("ALG-JUNE-LAB")).thenReturn(Optional.of(definition));
        when(examDefinitionRepository.findById("MISSING")).thenReturn(Optional.empty());

        assertThat(service.findById("ALG-JUNE-LAB")).isSameAs(definition);
        assertThrows(NoSuchElementException.class, () -> service.findById("MISSING"));
    }

    @Test
    void saveCreatesDefinitionsForEachExamSessionWhenMissing() {
        JoinedSubject subject = new JoinedSubject();
        subject.setAbbreviation("ALG");
        when(joinedSubjectRepository.findByAbbreviation("ALG")).thenReturn(Optional.of(subject));
        when(examDefinitionRepository.findById(any())).thenReturn(Optional.empty());
        when(examDefinitionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.save("ALG", 90L, "CLASSROOM", "note");

        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void editThrowsWhenSubjectIsMissing() {
        when(joinedSubjectRepository.findByAbbreviation("ALG")).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.edit("ALG-JUNE-CLASSROOM", "ALG", 90L, "CLASSROOM", "note"));
    }

    @Test
    void deleteByIdRemovesDefinitionAndAssociatedExams() {
        JoinedSubject subject = new JoinedSubject();
        subject.setAbbreviation("ALG");
        ExamDefinition definition = new ExamDefinition();
        definition.setId("ALG-JUNE-CLASSROOM");
        definition.setSubject(subject);
        definition.setExamSession(ExamSession.JUNE);

        SubjectExam exam = new SubjectExam(definition, new YearExamSession());
        exam.setId("2025-26-JUNE-ALG-JUNE-CLASSROOM");

        doReturn(Optional.of(definition), Optional.empty()).when(examDefinitionRepository).findById(definition.getId());
        when(subjectExamService.findAllByExamDefinitionAndExamSession(definition, ExamSession.JUNE)).thenReturn(List.of(exam));
        when(subjectExamService.delete(exam.getId())).thenReturn(exam);
        doNothing().when(examDefinitionRepository).deleteById(definition.getId());

        assertThat(service.deleteById(definition.getId())).isTrue();
    }
}




