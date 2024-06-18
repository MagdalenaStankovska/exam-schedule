package mk.ukim.finki.exam_schedule.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
public class YearExamSession {

    // 2022-23-JUNE
    @Id
    private String name;

    @Enumerated(EnumType.STRING)
    private ExamSession session;

    // 2022-23
    private String year;

    private LocalDate sessionStart;

    private LocalDate sessionEnd;

    private LocalDate enrollmentStartDate;

    private LocalDate enrollmentEndDate;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    private List<StudyCycle> cycle;


    public YearExamSession(ExamSession session, String year, LocalDate sessionStart, LocalDate sessionEnd, LocalDate enrollmentStartDate, LocalDate enrollmentEndDate, List<StudyCycle> cycle) {
        this.session = session;
        this.name = String.format("%s-%s", year.replace("/", "-"), session.name());
        this.year = year;
        this.sessionStart = sessionStart;
        this.sessionEnd = sessionEnd;
        this.enrollmentStartDate = enrollmentStartDate;
        this.enrollmentEndDate = enrollmentEndDate;
        this.cycle = cycle;
    }
}
