package mk.ukim.finki.exam_schedule.service;

import mk.ukim.finki.exam_schedule.model.JoinedSubject;
import mk.ukim.finki.exam_schedule.repository.JoinedSubjectRepository;
import mk.ukim.finki.exam_schedule.service.impl.JoinedSubjectServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JoinedSubjectServiceImplTest {

    @Mock
    private JoinedSubjectRepository joinedSubjectRepository;

    private JoinedSubjectServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new JoinedSubjectServiceImpl(joinedSubjectRepository);
    }

    @Test
    void findByIdReturnsMatchingSubject() {
        JoinedSubject subject = new JoinedSubject();
        subject.setAbbreviation("ALG");
        when(joinedSubjectRepository.findByAbbreviation("ALG")).thenReturn(Optional.of(subject));

        assertThat(service.findById("ALG")).isSameAs(subject);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(joinedSubjectRepository.findByAbbreviation("MISSING")).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class, () -> service.findById("MISSING"));
    }

    @Test
    void findAllReturnsAllSubjects() {
        JoinedSubject subject = new JoinedSubject();
        subject.setAbbreviation("ALG");
        when(joinedSubjectRepository.findAll()).thenReturn(List.of(subject));

        assertThat(service.findAll()).containsExactly(subject);
    }

    @Test
    void findPageDelegatesToRepository() {
        Page<JoinedSubject> page = new PageImpl<>(List.of(new JoinedSubject()));
        when(joinedSubjectRepository.findAll(any(Specification.class), any())).thenReturn(page);

        assertThat(service.findPage(1, 10, Specification.where(null))).isEqualTo(page);
    }
}


