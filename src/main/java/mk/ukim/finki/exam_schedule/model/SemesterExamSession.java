package mk.ukim.finki.exam_schedule.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@ToString
//@RequiredArgsConstructor
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class SemesterExamSession {

    // 2022/2023-W-JUNE
    @Id
    private String name;

    @Enumerated(EnumType.STRING)
    private ExamSession session;

    @ManyToOne
    private Semester semester;

    private LocalDate start;

    private LocalDate end;
}
