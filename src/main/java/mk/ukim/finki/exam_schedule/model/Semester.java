package mk.ukim.finki.exam_schedule.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.Hibernate;

import java.util.Objects;

@Getter
@Setter
@ToString

@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Semester {

    // 2022/2023-W
    @Id
    private String code;

    // 2022/2023
    private String year;

    @Enumerated(EnumType.STRING)
    private SemesterType semesterType;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Semester semester = (Semester) o;
        return getCode() != null && Objects.equals(getCode(), semester.getCode());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
