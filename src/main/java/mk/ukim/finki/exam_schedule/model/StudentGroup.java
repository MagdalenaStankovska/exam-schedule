package mk.ukim.finki.exam_schedule.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
public class StudentGroup {

    @Id
    @GeneratedValue
    private Long id;

    private String programs;

    private String name;

    private Short studyYear;

    private String lastNameRegex;

    @ManyToOne
    private Semester semester;
}