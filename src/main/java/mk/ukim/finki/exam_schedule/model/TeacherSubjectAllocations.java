package mk.ukim.finki.exam_schedule.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;

import java.util.Objects;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@JsonPropertyOrder({"semesterCode", "subjectId", "professorId",
        "englishGroup", "numberOfLectureGroups", "numberOfExerciseGroups", "numberOfLabGroups"})
public class TeacherSubjectAllocations {

    @Id
    @GeneratedValue
    private Long id;

    @JsonIgnore
    @ManyToOne
    private Professor professor;

    @Column(name = "professor_id", insertable = false, updatable = false)
    private String professorId;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "subject_id")
    private JoinedSubject subject;

    @Column(name = "subject_id", insertable = false, updatable = false)
    private String subjectId;

    @JsonIgnore
    @ManyToOne
    private Semester semester;

    @Column(name = "semester_code", updatable = false, insertable = false)
    private String semesterCode;

    private Boolean englishGroup;

    @Column(length = 4_000)
    private String validationMessage;


    private Float numberOfLectureGroups;
    private Float numberOfExerciseGroups;
    private Float numberOfLabGroups;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        TeacherSubjectAllocations that = (TeacherSubjectAllocations) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}

