package mk.ukim.finki.exam_schedule.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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
