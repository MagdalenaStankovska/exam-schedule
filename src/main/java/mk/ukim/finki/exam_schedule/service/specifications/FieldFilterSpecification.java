package mk.ukim.finki.exam_schedule.service.specifications;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import mk.ukim.finki.exam_schedule.model.ExamSession;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class FieldFilterSpecification {

    public static <T> Specification<T> filterEquals(Class<T> clazz, String field, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(fieldToPath(field, root), value);
    }

    public static <T, E extends Enum> Specification<T> filterEnumEquals(Class<T> clazz, String field, E value) {
        if (value == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(fieldToPath(field, root), value);
    }

    public static <T> Specification<T> filterExamSession(Class<T> entityClass, String attributeName, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        return (root, query, builder) -> builder.equal(root.get(attributeName), ExamSession.valueOf(value));
    }

    public static <T, V extends Comparable> Specification<T> greaterThen(Class<T> clazz, String field, V value) {
        if (value == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThan(fieldToPath(field, root), value);
    }

    public static <T, V> Specification<T> filterEqualsV(Class<T> clazz, String field, V value) {
        if (value == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(fieldToPath(field, root), value);
    }

    public static <T> Specification<T> filterEquals(Class<T> clazz, String field, Long value) {
        if (value == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(fieldToPath(field, root), value);
    }

    public static <T> Specification<T> filterContainsText(Class<T> clazz, String field, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(fieldToPath(field, root), "%" + value + "%");
    }

    public static <T> Specification<T> filterContainsTextCaseInsensitive(Class<T> clazz, String field, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(fieldToPath(field, root)),
                        "%" + value.toLowerCase() + "%"
                );
    }

    public static <T> Specification<T> filterFieldNotInList(Class<T> clazz, String field, List<?> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return (root, query, criteriaBuilder) -> {
            Path<Object> fieldPath = fieldToPath(field, root);
            return criteriaBuilder.not(fieldPath.in(values));
        };
    }

    private static <T> Path fieldToPath(String field, Root<T> root) {
        String[] parts = field.split("\\.");
        Path res = root;
        for (String p : parts) {
            res = res.get(p);
        }
        return res;
    }
}
