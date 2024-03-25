package mk.ukim.finki.exam_schedule.service.impl;

import mk.ukim.finki.exam_schedule.model.JoinedSubject;
import mk.ukim.finki.exam_schedule.repository.JoinedSubjectRepository;
import mk.ukim.finki.exam_schedule.service.JoinedSubjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import static org.springframework.data.domain.Sort.by;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class JoinedSubjectServiceImpl implements JoinedSubjectService {

    private final JoinedSubjectRepository joinedSubjectRepository;

    public JoinedSubjectServiceImpl(JoinedSubjectRepository joinedSubjectRepository) {
        this.joinedSubjectRepository = joinedSubjectRepository;
    }

    @Override
    public JoinedSubject findById(String id) {
        return this.joinedSubjectRepository.findByAbbreviation(id).orElseThrow(NoSuchElementException::new);
    }

    @Override
    public List<JoinedSubject> findAll() {
        return joinedSubjectRepository.findAll();
    }

    @Override
    public Page<JoinedSubject> findPage(Integer page, Integer size, Specification<JoinedSubject> filter) {
        return this.joinedSubjectRepository.findAll(filter, PageRequest.of(page - 1, size,
                by("name")));
    }
}
