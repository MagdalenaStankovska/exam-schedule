package mk.ukim.finki.exam_schedule.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelBasicsTest {

    @Test
    void userRoleHelperMethodsWorkForProfessorStudentAndAdminRoles() {
        assertThat(UserRole.PROFESSOR.isProfessor()).isTrue();
        assertThat(UserRole.PROFESSOR.isStudent()).isFalse();
        assertThat(UserRole.PROFESSOR.roleName()).isEqualTo("ROLE_PROFESSOR");

        assertThat(UserRole.STUDENT.isProfessor()).isFalse();
        assertThat(UserRole.STUDENT.isStudent()).isTrue();
        assertThat(UserRole.STUDENT.roleName()).isEqualTo("ROLE_STUDENT");

        assertThat(UserRole.DEAN.isProfessor()).isTrue();
        assertThat(UserRole.DEAN.isStudent()).isFalse();
        assertThat(UserRole.DEAN.roleName()).isEqualTo("ROLE_DEAN");
    }

    @Test
    void yearExamSessionConstructorSetsDerivedFields() {
        YearExamSession session = new YearExamSession(
                ExamSession.JUNE,
                "2025/26",
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 20),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 10),
                List.of(StudyCycle.UNDERGRADUATE, StudyCycle.MASTER)
        );

        assertThat(session.getName()).isEqualTo("2025-26-JUNE");
        assertThat(session.getSession()).isEqualTo(ExamSession.JUNE);
        assertThat(session.getYear()).isEqualTo("2025/26");
        assertThat(session.getSubmissionDeadline()).isEqualTo(LocalDate.of(2026, 5, 20));
        assertThat(session.getCycle()).containsExactly(StudyCycle.UNDERGRADUATE, StudyCycle.MASTER);
    }

    @Test
    void subjectExamConstructorBuildsIdAndDraftWorkflowStatus() {
        JoinedSubject subject = new JoinedSubject();
        subject.setAbbreviation("WP");
        subject.setName("Web Programming");

        YearExamSession session = new YearExamSession();
        session.setName("2025-26-JUNE");

        ExamDefinition definition = new ExamDefinition();
        definition.setId("WP-JUNE-LAB");
        definition.setSubject(subject);

        SubjectExam exam = new SubjectExam(definition, session);

        assertThat(exam.getId()).isEqualTo("2025-26-JUNE-WP-JUNE-LAB");
        assertThat(exam.getDefinition()).isSameAs(definition);
        assertThat(exam.getSession()).isSameAs(session);
        assertThat(exam.getWorkflowStatus()).isEqualTo(ExamWorkflowStatus.DRAFT);
    }

    @Test
    void subjectAllocationStatsHelperMethodsComputeTotalsAndIds() {
        Semester semester = new Semester("2025/26", "W");
        JoinedSubject subject = new JoinedSubject();
        subject.setAbbreviation("ALG");

        SubjectAllocationStats stats = new SubjectAllocationStats(semester, subject);
        stats.setNumberOfFirstTimeStudents(120);
        stats.setNumberOfReEnrollmentStudents(30);

        assertThat(stats.getId()).isEqualTo("2025/26-W-ALG");
        assertThat(stats.getTotalStudents()).isEqualTo(150);
        assertThat(SubjectAllocationStats.constructId(semester, subject)).isEqualTo("2025/26-W-ALG");
        assertThat(SubjectAllocationStats.constructId("2025/26-W", "ALG")).isEqualTo("2025/26-W-ALG");
    }
}

