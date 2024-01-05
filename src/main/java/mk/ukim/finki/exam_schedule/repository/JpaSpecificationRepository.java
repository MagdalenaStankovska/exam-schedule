package mk.ukim.finki.exam_schedule.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface JpaSpecificationRepository<Y, S> extends JpaRepository<Y, String> {

    Page<Y> findAll(Specification<Y> filter, Pageable page);

    List<Y> findAll(Specification<Y> filter);
}
