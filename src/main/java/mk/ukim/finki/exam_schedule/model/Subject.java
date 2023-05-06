package mk.ukim.finki.exam_schedule.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;

import java.util.Objects;

@Getter
@Setter
@ToString
<<<<<<< HEAD
//@RequiredArgsConstructor
=======
>>>>>>> 11fd539188f35f2b8a5e569e419a7ee66f986eae
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Subject {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private SemesterType semester;

    private Integer weeklyLecturesClasses;

    private Integer weeklyAuditoriumClasses;

    private Integer weeklyLabClasses;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Subject subject = (Subject) o;
        return getId() != null && Objects.equals(getId(), subject.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
