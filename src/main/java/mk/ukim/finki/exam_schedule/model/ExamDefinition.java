package mk.ukim.finki.exam_schedule.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class ExamDefinition extends TrackedHistoryEntity {

    // WP-JUNE-LAB
    @Id
    private String id;

    @ManyToOne
    JoinedSubject subject;

    @Enumerated(EnumType.STRING)
    private ExamSession examSession;

    private Long durationMinutes;

    @Enumerated(EnumType.STRING)
    private ExamType type;

    private String note;

}
