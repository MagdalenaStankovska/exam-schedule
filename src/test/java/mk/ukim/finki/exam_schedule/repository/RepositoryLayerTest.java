package mk.ukim.finki.exam_schedule.repository;

import mk.ukim.finki.exam_schedule.model.*;
import mk.ukim.finki.exam_schedule.service.specifications.FieldFilterSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RepositoryLayerTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private JoinedSubjectRepository joinedSubjectRepository;
    @Autowired
    private ExamDefinitionRepository examDefinitionRepository;
    @Autowired
    private YearExamSessionRepository yearExamSessionRepository;
    @Autowired
    private SubjectExamRepository subjectExamRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private SemesterRepository semesterRepository;
    @Autowired
    private SubjectAllocationStatsRepository subjectAllocationStatsRepository;
    @Autowired
    private StudentCoursesRepository studentCoursesRepository;
    @Autowired
    private TeacherSubjectAllocationsRepository teacherSubjectAllocationsRepository;
    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Test
    void flywayMigrationCreatedTimeSlotTable() {
        assertThat(timeSlotRepository.count()).isZero();
    }

    @Test
    void userRepositoryFindByEmailWorks() {
        User user = entityManager.persistFlushFind(new User("u1", "Admin", "admin@example.com", UserRole.DEAN));

        assertThat(userRepository.findByEmail("admin@example.com")).contains(user);
    }

    @Test
    void roomRepositoryCustomQueriesWork() {
        Room classroom = entityManager.persistFlushFind(new Room("A1", "Main", "projector", RoomType.CLASSROOM, 100L));
        Room lab = entityManager.persistFlushFind(new Room("L1", "Lab", "pcs", RoomType.LAB, 20L));

        assertThat(roomRepository.findAllByNameIn(Set.of("A1", "L1"))).containsExactlyInAnyOrder(classroom, lab);
        assertThat(roomRepository.findAllByType(RoomType.LAB)).containsExactly(lab);
        assertThat(roomRepository.findAllByOrderByNameAsc()).extracting(Room::getName).containsExactly("A1", "L1");
    }

    @Test
    void joinedSubjectRepositoryFindByAbbreviationWorks() {
        JoinedSubject subject = new JoinedSubject();
        subject.setAbbreviation("ALG");
        subject.setName("Algorithms");
        subject.setSemesterType(SemesterType.WINTER);
        entityManager.persistAndFlush(subject);

        assertThat(joinedSubjectRepository.findByAbbreviation("ALG")).contains(subject);
    }

    @Test
    void examDefinitionRepositorySpecificationQueryWorks() {
        JoinedSubject subject = new JoinedSubject();
        subject.setAbbreviation("ALG");
        subject.setName("Algorithms");
        subject.setSemesterType(SemesterType.WINTER);
        subject = entityManager.persistAndFlush(subject);
        ExamDefinition definition = new ExamDefinition("ALG-JUNE-CLASSROOM", subject, ExamSession.JUNE, 90L, ExamType.CLASSROOM, "core");
        entityManager.persistAndFlush(definition);

        assertThat(examDefinitionRepository.findAll(
                FieldFilterSpecification.filterEnumEquals(ExamDefinition.class, "type", ExamType.CLASSROOM),
                PageRequest.of(0, 10)
        ).getContent()).containsExactly(definition);
    }

    @Test
    void yearExamSessionRepositorySpecificationQueryWorks() {
        YearExamSession session = new YearExamSession(ExamSession.JUNE, "2025/26", LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 20), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10), List.of(StudyCycle.UNDERGRADUATE));
        entityManager.persistAndFlush(session);

        assertThat(yearExamSessionRepository.findAll(
                FieldFilterSpecification.filterEquals(YearExamSession.class, "year", "2025/26"),
                PageRequest.of(0, 10)
        ).getContent()).containsExactly(session);
    }

    @Test
    void subjectExamRepositoryCustomQueriesWork() {
        JoinedSubject subject = new JoinedSubject();
        subject.setAbbreviation("ALG");
        subject.setName("Algorithms");
        subject.setSemesterType(SemesterType.WINTER);
        subject = entityManager.persistAndFlush(subject);
        YearExamSession session = new YearExamSession(ExamSession.JUNE, "2025/26", LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 20), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10), List.of(StudyCycle.UNDERGRADUATE));
        entityManager.persistAndFlush(session);
        ExamDefinition definition = entityManager.persistAndFlush(new ExamDefinition("ALG-JUNE-CLASSROOM", subject, ExamSession.JUNE, 90L, ExamType.CLASSROOM, "core"));
        Room room = entityManager.persistAndFlush(new Room("A1", "Main", "projector", RoomType.CLASSROOM, 100L));
        TimeSlot slot = entityManager.persistAndFlush(new TimeSlot(LocalDateTime.of(2026, 6, 11, 10, 0), LocalDateTime.of(2026, 6, 11, 11, 30)));

        SubjectExam exam = new SubjectExam(definition, session);
        exam.setFromTime(slot.getFromTime());
        exam.setToTime(slot.getToTime());
        exam.setTimeSlot(slot);
        exam.setRooms(Set.of(room));
        exam.setWorkflowStatus(ExamWorkflowStatus.SCHEDULED);
        entityManager.persistAndFlush(exam);

        assertThat(subjectExamRepository.findAllBySession(session)).containsExactly(exam);
        assertThat(subjectExamRepository.findAllByDefinitionAndSessionSession(definition, ExamSession.JUNE)).containsExactly(exam);
        assertThat(subjectExamRepository.findByDefinition_Subject(subject)).containsExactly(exam);
        assertThat(subjectExamRepository.findBySessionCycle(StudyCycle.UNDERGRADUATE)).containsExactly(exam);
        assertThat(subjectExamRepository.findByRoomsContaining(room)).containsExactly(exam);
        assertThat(subjectExamRepository.findByIdWithRooms(exam.getId())).contains(exam);
    }

    @Test
    void courseSemesterAndStudentRelationshipQueriesWork() {
        Semester semester = entityManager.persistAndFlush(new Semester("2025/26", "W"));
        JoinedSubject subject = new JoinedSubject();
        subject.setAbbreviation("ALG");
        subject.setName("Algorithms");
        subject.setSemesterType(SemesterType.WINTER);
        subject = entityManager.persistAndFlush(subject);

        Course course = new Course();
        course.setSemester(semester);
        course.setJoinedSubject(subject);
        course.setNumberOfFirstEnrollments(20);
        course.setNumberOfReEnrollments(5);
        course = entityManager.persistAndFlush(course);

        // 1. Прво креирај и зачувај ја студиската програма во база
        StudyProgram studyProgram = entityManager.persistAndFlush(new StudyProgram("CS", "CS"));

        // 2. Сега пренеси ја веќе зачуваната програма во Студентот
        Student student = entityManager.persistAndFlush(new Student("12345", "s@example.com", "S", "T", "P", studyProgram));
        StudentCourses link = entityManager.persistAndFlush(new StudentCourses(null, student, course));

        assertThat(semesterRepository.findById(semester.getCode())).contains(semester);
        assertThat(courseRepository.findAllBySemester(semester)).containsExactly(course);
        assertThat(studentCoursesRepository.findAllByCourse_JoinedSubject_AbbreviationIn(Set.of("ALG"))).containsExactly(link);
    }

    @Test
    void allocationAndTeacherRepositoriesWork() {
        Semester semester = entityManager.persistAndFlush(new Semester("2025/26", "W"));
        JoinedSubject subject = new JoinedSubject();
        subject.setAbbreviation("ALG");
        subject.setName("Algorithms");
        subject.setSemesterType(SemesterType.WINTER);
        subject = entityManager.persistAndFlush(subject);
        SubjectAllocationStats stats = entityManager.persistAndFlush(new SubjectAllocationStats(semester, subject));
        stats.setNumberOfFirstTimeStudents(10);
        stats.setNumberOfReEnrollmentStudents(4);
        entityManager.persistAndFlush(stats);

        Professor professor = entityManager.persistAndFlush(new Professor("p1", "Prof", "prof@example.com", ProfessorTitle.PROFESSOR));
        TeacherSubjectAllocations allocation = new TeacherSubjectAllocations();
        allocation.setProfessor(professor);
        allocation.setSubject(subject);
        allocation.setSemester(semester);
        allocation.setEnglishGroup(Boolean.FALSE);
        allocation.setNumberOfLectureGroups(1.5F);
        allocation.setNumberOfExerciseGroups(2.0F);
        allocation.setNumberOfLabGroups(0.5F);
        allocation = entityManager.persistAndFlush(allocation);

        assertThat(subjectAllocationStatsRepository.findAllBySubject(subject)).containsExactly(stats);
        assertThat(teacherSubjectAllocationsRepository.findAllByProfessorId("p1")).containsExactly(allocation);
        assertThat(teacherSubjectAllocationsRepository.findAllByProfessorIdAndSubjectId("p1", "ALG")).containsExactly(allocation);
        assertThat(teacherSubjectAllocationsRepository.findAllBySubjectIdIn(Set.of("ALG"))).containsExactly(allocation);
    }

    @Test
    void timeSlotRepositoryPersistsAndFindsEntity() {
        TimeSlot slot = entityManager.persistFlushFind(new TimeSlot(LocalDateTime.of(2026, 6, 10, 10, 0), LocalDateTime.of(2026, 6, 10, 10, 30)));

        assertThat(timeSlotRepository.findById(slot.getId())).contains(slot);
    }

}



