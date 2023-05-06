package mk.ukim.finki.exam_schedule.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;

import java.util.Objects;

@Getter
@Setter
@ToString
//@RequiredArgsConstructor
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Course {

    @Id
    @Column(name = "id")
    private String id;

    @ManyToOne
    private Semester semester;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    private Long totalStudents;

    private Long totalTeachingStaff;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Course course = (Course) o;
        return getId() != null && Objects.equals(getId(), course.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
