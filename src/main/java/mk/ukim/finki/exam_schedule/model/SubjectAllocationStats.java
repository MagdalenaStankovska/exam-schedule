package mk.ukim.finki.exam_schedule.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;
import org.hibernate.Hibernate;

import java.util.Objects;
import java.util.stream.Stream;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
public class SubjectAllocationStats {

    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private JoinedSubject subject;

    @ManyToOne
    private Semester semester;

    private Integer numberOfTeachers = 0;

    private Integer numberOfFirstTimeStudents = 0;

    private Integer numberOfReEnrollmentStudents = 0;

    private Integer numberOfGroups = 0;

    private Integer calculatedNumberOfGroups;

    private Integer averageStudentsPerGroup;

    private Boolean mentorshipCourse = false;

    // to be calculated dynamically
    private Float coveredLectureGroups = 0F;

    private Float coveredExerciseGroups = 0F;

    private Float coveredLabGroups = 0F;


    public Integer getTotalStudents() {
        return (numberOfFirstTimeStudents != null ? numberOfFirstTimeStudents : 0) +
                (numberOfReEnrollmentStudents != null ? numberOfReEnrollmentStudents : 0);

    }

    public SubjectAllocationStats(Semester semester, JoinedSubject subject) {
        this.id = constructId(semester, subject);
        this.subject = subject;
        this.semester = semester;
    }

    public static String constructId(Semester semester, JoinedSubject subject) {
        return constructId(semester.getCode(), subject.getAbbreviation());
    }

    public static String constructId(String semesterCode, String subjectAbbreviation) {
        return String.format("%s-%s", semesterCode, subjectAbbreviation);
    }

    public static Float sumLectureGroups(Stream<TeacherSubjectAllocations> stream) {
        return stream.map(TeacherSubjectAllocations::getNumberOfLectureGroups).reduce(0.0F, Float::sum);
    }

    public static Float sumExerciseGroups(Stream<TeacherSubjectAllocations> stream) {
        return stream.map(TeacherSubjectAllocations::getNumberOfExerciseGroups).reduce(0.0F, Float::sum);
    }

    public static Float sumLabGroups(Stream<TeacherSubjectAllocations> stream) {
        return stream.map(TeacherSubjectAllocations::getNumberOfLabGroups).reduce(0.0F, Float::sum);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        SubjectAllocationStats that = (SubjectAllocationStats) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}



