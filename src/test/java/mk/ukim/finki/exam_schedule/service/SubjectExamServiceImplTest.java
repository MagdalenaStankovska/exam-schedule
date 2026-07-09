package mk.ukim.finki.exam_schedule.service;

import mk.ukim.finki.exam_schedule.model.ExamDefinition;
import mk.ukim.finki.exam_schedule.model.JoinedSubject;
import mk.ukim.finki.exam_schedule.model.SubjectExam;
import mk.ukim.finki.exam_schedule.model.TeacherSubjectAllocations;
import mk.ukim.finki.exam_schedule.model.User;
import mk.ukim.finki.exam_schedule.repository.ExamDefinitionRepository;
import mk.ukim.finki.exam_schedule.repository.JoinedSubjectRepository;
import mk.ukim.finki.exam_schedule.repository.SubjectExamRepository;
import mk.ukim.finki.exam_schedule.repository.TeacherSubjectAllocationsRepository;
import mk.ukim.finki.exam_schedule.repository.TimeSlotRepository;
import mk.ukim.finki.exam_schedule.repository.YearExamSessionRepository;
import mk.ukim.finki.exam_schedule.service.impl.SubjectExamServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests the private method {@code canManageExam(SubjectExam)} in
 * {@link SubjectExamServiceImpl} through reflection, targeting Prime Path,
 * Edge-Pair, and RACC/GACC (logic) coverage of the decision:
 *
 * <pre>if (!hasProfessorRole && !currentUserService.isProfessor())</pre>
 *
 * Clause A = hasProfessorRole (derived from the Spring Security authorities
 * already present on the SecurityContext, e.g. set by
 * HeaderUserAuthenticationFilter).
 * Clause B = currentUserService.isProfessor() (the domain-level role check).
 *
 * Note on short-circuit evaluation: because the guard is a "&&" and the
 * ROLE_ADMIN branch above it is an "||", Mockito must only be stubbed for
 * calls that are actually reached, or MockitoExtension's strict stubbing
 * will fail the test with UnnecessaryStubbingException. This class stubs
 * exactly (and only) what each branch evaluates.
 */
@ExtendWith(MockitoExtension.class)
class SubjectExamServiceImplTest {

    @Mock
    private SubjectExamRepository subjectExamRepository;
    @Mock
    private YearExamSessionRepository yearExamSessionRepository;
    @Mock
    private ExamDefinitionRepository examDefinitionRepository;
    @Mock
    private JoinedSubjectRepository joinedSubjectRepository;
    @Mock
    private SubjectAllocationStatsService subjectAllocationStatsService;
    @Mock
    private TeacherSubjectAllocationsRepository teacherSubjectAllocationsRepository;
    @Mock
    private TimeSlotRepository timeSlotRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private RoomService roomService;

    private SubjectExamServiceImpl subjectExamService;

    private static final String PROFESSOR_ID = "prof-1";
    private static final String SUBJECT_ID = "SUBJ-PROF";

    @BeforeEach
    void setUp() {
        subjectExamService = new SubjectExamServiceImpl(
                subjectExamRepository, yearExamSessionRepository, examDefinitionRepository,
                joinedSubjectRepository, subjectAllocationStatsService, teacherSubjectAllocationsRepository,
                timeSlotRepository, currentUserService, roomService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private SubjectExam examForSubject(String subjectAbbreviation) {
        JoinedSubject subject = new JoinedSubject();
        subject.setAbbreviation(subjectAbbreviation);

        ExamDefinition definition = new ExamDefinition();
        definition.setSubject(subject);

        SubjectExam exam = new SubjectExam();
        exam.setDefinition(definition);
        return exam;
    }

    private void authenticateWithAuthorities(String... authorities) {
        List<SimpleGrantedAuthority> grantedAuthorities = List.of(authorities).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        Authentication authentication =
                new UsernamePasswordAuthenticationToken("test-user", null, grantedAuthorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private boolean invokeCanManageExam(SubjectExam exam) {
        return ReflectionTestUtils.invokeMethod(subjectExamService, "canManageExam", exam);
    }

    // --- Row 1 of the truth table (A=T,B=T) is subsumed by Row 2/short-circuit
    //     on the ROLE_ADMIN OR-branch above; not separately exercised here. ---

    /**
     * hasAdminRole = true (via SecurityContext authority ROLE_ADMIN).
     * Because of "hasAdminRole || currentUserService.isAdmin()", the second
     * operand is short-circuited and MUST NOT be stubbed.
     */
    @Test
    void canManageExam_shouldReturnTrueForAdminRole() {
        authenticateWithAuthorities("ROLE_ADMIN");
        SubjectExam exam = examForSubject(SUBJECT_ID);

        boolean result = invokeCanManageExam(exam);

        assertThat(result).isTrue();
    }

    /**
     * Truth-table Row 4: A=false, B=false.
     * hasProfessorRole = false (no matching authority) AND
     * currentUserService.isProfessor() = false -> guard triggers -> false.
     * Both currentUserService.isAdmin() and isProfessor() are genuinely
     * evaluated here (no short-circuit), so both are legitimately stubbed.
     */
    @Test
    void canManageExam_shouldReturnFalseWhenUserIsNotAdminAndNotProfessor() {
        authenticateWithAuthorities(); // no authorities at all
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.isProfessor()).thenReturn(false);
        SubjectExam exam = examForSubject(SUBJECT_ID);

        boolean result = invokeCanManageExam(exam);

        assertThat(result).isFalse();
    }

    /**
     * Truth-table Row 2: A=true, B=false (B is irrelevant/short-circuited).
     * hasProfessorRole = true via SecurityContext ROLE_PROFESSOR authority,
     * so currentUserService.isProfessor() is NEVER called (must not be
     * stubbed). The guard is skipped, and the method falls through to the
     * allocation lookup, which here returns empty -> false.
     */
    @Test
    void canManageExam_shouldReturnFalseForProfessorWithoutAllocations() {
        authenticateWithAuthorities("ROLE_PROFESSOR");
        when(currentUserService.isAdmin()).thenReturn(false);
        User professor = new User(PROFESSOR_ID, "Prof", "prof@example.com", null);
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(professor));
        when(teacherSubjectAllocationsRepository.findAllByProfessorIdAndSubjectId(PROFESSOR_ID, SUBJECT_ID))
                .thenReturn(Collections.emptyList());
        SubjectExam exam = examForSubject(SUBJECT_ID);

        boolean result = invokeCanManageExam(exam);

        assertThat(result).isFalse();
    }

    /**
     * Truth-table Row 3: A=false, B=true - the only test where clause B
     * (currentUserService.isProfessor()) is the ACTUAL deciding factor.
     * Deliberately no ROLE_PROFESSOR authority is granted at the
     * SecurityContext level (hasProfessorRole = false), forcing real
     * evaluation of currentUserService.isProfessor() = true. This is what
     * makes the RACC/GACC claim for clause B genuinely true, unlike a test
     * that grants ROLE_PROFESSOR directly (which would short-circuit B).
     */
    @Test
    void canManageExam_shouldReturnTrueForProfessorWithAllocations() {
        authenticateWithAuthorities(); // no ROLE_PROFESSOR authority on purpose
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.isProfessor()).thenReturn(true);
        User professor = new User(PROFESSOR_ID, "Prof", "prof@example.com", null);
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(professor));
        TeacherSubjectAllocations allocation = new TeacherSubjectAllocations();
        when(teacherSubjectAllocationsRepository.findAllByProfessorIdAndSubjectId(PROFESSOR_ID, SUBJECT_ID))
                .thenReturn(List.of(allocation));
        SubjectExam exam = examForSubject(SUBJECT_ID);

        boolean result = invokeCanManageExam(exam);

        assertThat(result).isTrue();
    }
}