package mk.ukim.finki.exam_schedule.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Professors fill this entity for each of the parts of their courses
 */
@Getter
@Setter
@ToString

@NoArgsConstructor
@Entity
public class CourseExamPart {

    // 2022/2023-W-JUNE-2022/2023-OS-name
    @Id
    private String id;

    @ManyToOne
    private Course course;

    @ManyToOne
    private SemesterExamSession session;

    private String name; // prakticno/teorija...


    @Enumerated(EnumType.STRING)
    private ExamType type;

    private Long durationMinutes;

    private Long previousYearAttendantsNumber;

    private Long attendantsNumber;

    private Long numRepetitions; // termini ako ne go sobira vo site lab

    private LocalDateTime from;
    private LocalDateTime to;

    @ManyToMany
    private Set<Room> rooms;

    @Lob
    private String comment;

    public CourseExamPart(Course course, SemesterExamSession session, String name) {
        this.course = course;
        this.session = session;
        this.name = name;

        this.id = String.format("%s-%s-%s", session.getName(), course.getId(), name);
    }

}
