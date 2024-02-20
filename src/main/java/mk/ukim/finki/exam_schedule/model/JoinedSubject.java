package mk.ukim.finki.exam_schedule.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
public class JoinedSubject extends TrackedHistoryEntity {

    @Id
    private String abbreviation;

    @Column(length = 1000)
    private String name;

    private String codes;

    @Enumerated(EnumType.STRING)
    private SemesterType semesterType;

    @ManyToOne
    private Subject mainSubject;


    private Integer weeklyLecturesClasses;

    private Integer weeklyAuditoriumClasses;

    private Integer weeklyLabClasses;

    @Enumerated(EnumType.STRING)
    private StudyCycle cycle;
}
