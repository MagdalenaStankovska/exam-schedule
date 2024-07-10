package mk.ukim.finki.exam_schedule.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class SubjectExam {

    // 2022-23-JUNE-WP-JUNE-LAB
    //JUNE-2023-24-S-VEB3A-JUNE-LAB
    @Id
    private String id;

    @ManyToOne
    private YearExamSession session;

    @ManyToOne
    private ExamDefinition definition;

    private Long durationMinutes;

    private Long previousYearAttendantsNumber;
    private Long previousYearTotalStudents;

    private Long attendantsNumber;
    private Long totalStudents;

    // treba da se presmeta od (procenot prisutni lani + 5%) * studenti godinava
    private Long expectedNumber;

    private Long numRepetitions; // termini ako ne go sobira vo site lab

    private LocalDateTime fromTime;
    private LocalDateTime toTime;

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinTable(
            name = "subject_exam_rooms",
            joinColumns = @JoinColumn(name = "subject_exam_id"),
            inverseJoinColumns = @JoinColumn(name = "rooms_name")
    )
    private Set<Room> rooms;

    @Column(length = 5000)
    private String comment;

    public SubjectExam(ExamDefinition definition, YearExamSession session) {
        this.definition = definition;
        this.session = session;


        this.id = String.format("%s-%s", session.getName(), definition.getId());
    }
}
