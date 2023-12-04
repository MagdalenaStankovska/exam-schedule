//package mk.ukim.finki.exam_schedule.model;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.time.LocalDate;
//import java.util.List;
//
//@Getter
//@Setter
//@ToString
//@NoArgsConstructor
//@AllArgsConstructor
//@Entity
//public class SemesterExamSession {
//
//    // 2022-23-JUNE
//    @Id
//    private String name;
//
//    @Enumerated(EnumType.STRING)
//    private ExamSession session;
//
//    @Deprecated
//    @ManyToOne
//    private Semester semester;
//
//    // 2022-23
//    private String year;
//
//    private LocalDate start;
//
//    // end is a reserved key for SQL, need to change it
//    private LocalDate end;
//
//    private LocalDate enrollmentStartDate;
//
//    private LocalDate enrollmentEndDate;
//
//    @ElementCollection
//    @Enumerated(EnumType.STRING)
//    private List<StudyCycle> cycle;
//
//
//}
