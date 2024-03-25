package mk.ukim.finki.exam_schedule.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.Optional;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
public class Course {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Semester semester;

    @ManyToOne
    private JoinedSubject joinedSubject;

    @ManyToOne
    private Professor professor;

    @ManyToOne
    private Professor assistant;

    private String professors;

    private String assistants;

    @ManyToMany
    private List<StudentGroup> studentGroups;

    @ManyToMany
    private List<Room> rooms;

    private Integer numberOfFirstEnrollments;

    private Integer numberOfReEnrollments;

    private Float groupPortion = 1.0F;

    private String groups;

    private Boolean english;

    public Integer getTotalStudents() {
        return Optional.ofNullable(numberOfFirstEnrollments).orElse(0) +
                Optional.ofNullable(numberOfReEnrollments).orElse(0);
    }
}
