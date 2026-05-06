package mk.ukim.finki.exam_schedule.service.scheduling.impl;

import mk.ukim.finki.exam_schedule.model.*;
import mk.ukim.finki.exam_schedule.repository.StudentCoursesRepository;
import mk.ukim.finki.exam_schedule.repository.SubjectExamRepository;
import mk.ukim.finki.exam_schedule.repository.TeacherSubjectAllocationsRepository;
import mk.ukim.finki.exam_schedule.repository.TimeSlotRepository;
import mk.ukim.finki.exam_schedule.repository.YearExamSessionRepository;
import mk.ukim.finki.exam_schedule.service.RoomService;
import mk.ukim.finki.exam_schedule.service.scheduling.GeminiScheduleClient;
import mk.ukim.finki.exam_schedule.service.scheduling.GeminiSuggestion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleGenerationServiceImplTest {

    @Mock
    private SubjectExamRepository subjectExamRepository;
    @Mock
    private YearExamSessionRepository yearExamSessionRepository;
    @Mock
    private TimeSlotRepository timeSlotRepository;
    @Mock
    private TeacherSubjectAllocationsRepository teacherSubjectAllocationsRepository;
    @Mock
    private StudentCoursesRepository studentCoursesRepository;
    @Mock
    private RoomService roomService;
    @Mock
    private GeminiScheduleClient geminiScheduleClient;

    private ScheduleGenerationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ScheduleGenerationServiceImpl(
                subjectExamRepository,
                yearExamSessionRepository,
                timeSlotRepository,
                teacherSubjectAllocationsRepository,
                studentCoursesRepository,
                roomService,
                geminiScheduleClient
        );

        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(geminiScheduleClient.suggestSchedule(any(String.class), ArgumentMatchers.<List<SubjectExam>>any()))
                .thenReturn(Collections.<GeminiSuggestion>emptyList());
        when(studentCoursesRepository.findAllByCourse_JoinedSubject_AbbreviationIn(any())).thenReturn(Collections.emptyList());
    }

    @Test
    void shouldSplitLargeExamAcrossMultipleRoomsAtSameTime() {
        YearExamSession session = createSession("2025-26-JUNE");
        SubjectExam exam = createExam("DATABASE", session, SemesterType.SUMMER, 300L, 120L, ExamType.CLASSROOM, "core");

        List<Room> rooms = List.of(
                new Room("A1", "A", "", RoomType.CLASSROOM, 100L),
                new Room("A2", "A", "", RoomType.CLASSROOM, 100L),
                new Room("A3", "A", "", RoomType.CLASSROOM, 100L)
        );

        when(yearExamSessionRepository.findById(session.getName())).thenReturn(java.util.Optional.of(session));
        when(subjectExamRepository.findAllBySession(session)).thenReturn(List.of(exam));
        when(roomService.findAllByRoomType(RoomType.CLASSROOM)).thenReturn(rooms);
        when(teacherSubjectAllocationsRepository.findAllBySubjectIdIn(any())).thenReturn(Collections.emptyList());
        when(subjectExamRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<SubjectExam> generated = service.generateForSession(session.getName());

        Assertions.assertEquals(1, generated.size());
        SubjectExam scheduled = generated.get(0);
        Assertions.assertNotNull(scheduled.getFromTime());
        Assertions.assertNotNull(scheduled.getToTime());
        Assertions.assertEquals(3, scheduled.getRooms().size());
        Assertions.assertEquals(1L, scheduled.getNumRepetitions());
        Assertions.assertEquals(ExamWorkflowStatus.SCHEDULED, scheduled.getWorkflowStatus());
    }

    @Test
    void shouldPreventProfessorConflictAcrossDifferentSemesters() {
        YearExamSession session = createSession("2025-26-SEPTEMBER");

        SubjectExam examOne = createExam("SUBJ1", session, SemesterType.WINTER, 60L, 60L, ExamType.CLASSROOM, "");
        SubjectExam examTwo = createExam("SUBJ2", session, SemesterType.SUMMER, 60L, 60L, ExamType.CLASSROOM, "");

        TeacherSubjectAllocations allocationOne = new TeacherSubjectAllocations();
        allocationOne.setSubjectId("SUBJ1");
        allocationOne.setProfessorId("prof-1");

        TeacherSubjectAllocations allocationTwo = new TeacherSubjectAllocations();
        allocationTwo.setSubjectId("SUBJ2");
        allocationTwo.setProfessorId("prof-1");

        List<Room> rooms = List.of(
                new Room("B1", "B", "", RoomType.CLASSROOM, 200L),
                new Room("B2", "B", "", RoomType.CLASSROOM, 200L)
        );

        when(yearExamSessionRepository.findById(session.getName())).thenReturn(java.util.Optional.of(session));
        when(subjectExamRepository.findAllBySession(session)).thenReturn(List.of(examOne, examTwo));
        when(roomService.findAllByRoomType(RoomType.CLASSROOM)).thenReturn(rooms);
        when(teacherSubjectAllocationsRepository.findAllBySubjectIdIn(any())).thenReturn(List.of(allocationOne, allocationTwo));
        when(subjectExamRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<SubjectExam> generated = service.generateForSession(session.getName());

        SubjectExam first = generated.get(0);
        SubjectExam second = generated.get(1);

        Assertions.assertNotNull(first.getFromTime());
        Assertions.assertNotNull(second.getFromTime());
        Assertions.assertFalse(first.getFromTime().isBefore(second.getToTime()) && second.getFromTime().isBefore(first.getToTime()));
    }

    private YearExamSession createSession(String name) {
        YearExamSession session = new YearExamSession();
        session.setName(name);
        session.setSessionStart(LocalDate.of(2026, 6, 10));
        session.setSessionEnd(LocalDate.of(2026, 6, 10));
        session.setCycle(List.of(StudyCycle.UNDERGRADUATE));
        return session;
    }

    private SubjectExam createExam(String subjectId,
                                   YearExamSession session,
                                   SemesterType semesterType,
                                   long expected,
                                   long durationMinutes,
                                   ExamType type,
                                   String note) {
        JoinedSubject subject = new JoinedSubject();
        subject.setAbbreviation(subjectId);
        subject.setName(subjectId + " name");
        subject.setSemesterType(semesterType);

        ExamDefinition definition = new ExamDefinition();
        definition.setId(subjectId + "-DEF");
        definition.setSubject(subject);
        definition.setType(type);
        definition.setDurationMinutes(durationMinutes);
        definition.setNote(note);

        SubjectExam exam = new SubjectExam(definition, session);
        exam.setExpectedNumber(expected);
        exam.setDurationMinutes(durationMinutes);
        exam.setWorkflowStatus(ExamWorkflowStatus.DRAFT);
        return exam;
    }
}

