package mk.ukim.finki.exam_schedule.service;

import mk.ukim.finki.exam_schedule.model.JoinedSubject;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface JoinedSubjectService {

    JoinedSubject findById(String id);

    List<JoinedSubject> findAll();

    Page<JoinedSubject> findPage(Integer page, Integer size, Specification<JoinedSubject> filter);
}
