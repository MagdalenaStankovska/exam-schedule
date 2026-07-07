package mk.ukim.finki.exam_schedule.web;

import mk.ukim.finki.exam_schedule.config.HeaderUserAuthenticationFilter;
import mk.ukim.finki.exam_schedule.config.SecurityConfig;
import mk.ukim.finki.exam_schedule.model.*;
import mk.ukim.finki.exam_schedule.repository.UserRepository;
import mk.ukim.finki.exam_schedule.service.*;
import mk.ukim.finki.exam_schedule.service.export.ScheduleExportService;
import mk.ukim.finki.exam_schedule.service.scheduling.ScheduleGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@WebMvcTest(controllers = {
        YearExamSessionController.class,
        ExamDefinitionController.class,
        ProfessorSubjectExamController.class,
        PublicScheduleController.class,
        ExamsCalendarController.class,
        SchedulingController.class,
        ErrorPageController.class
})
@Import({SecurityConfig.class, HeaderUserAuthenticationFilter.class})
class WebMvcSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.boot.test.mock.mockito.MockBean
    private YearExamSessionService yearExamSessionService;
    @org.springframework.boot.test.mock.mockito.MockBean
    private ExamDefinitionService examDefinitionService;
    @org.springframework.boot.test.mock.mockito.MockBean
    private JoinedSubjectService joinedSubjectService;
    @org.springframework.boot.test.mock.mockito.MockBean
    private SubjectExamService subjectExamService;
    @org.springframework.boot.test.mock.mockito.MockBean
    private RoomService roomService;
    @org.springframework.boot.test.mock.mockito.MockBean
    private CurrentUserService currentUserService;
    @org.springframework.boot.test.mock.mockito.MockBean
    private ScheduleExportService scheduleExportService;
    @org.springframework.boot.test.mock.mockito.MockBean
    private ScheduleGenerationService scheduleGenerationService;
    @org.springframework.boot.test.mock.mockito.MockBean
    private UserRepository userRepository;
    @org.springframework.boot.test.mock.mockito.MockBean
    private mk.ukim.finki.exam_schedule.repository.YearExamSessionRepository yearExamSessionRepository;
    @org.springframework.boot.test.mock.mockito.MockBean
    private mk.ukim.finki.exam_schedule.repository.SubjectExamRepository subjectExamRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminOnlyEndpointReturns200ForAdmin() throws Exception {
        when(yearExamSessionService.findAll(any(Specification.class), anyInt(), anyInt())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/admin/exam-session"))
                .andExpect(status().isOk())
                .andExpect(view().name("listYearExamSessions"));
    }

    @Test
    @WithMockUser(roles = "PROFESSOR")
    void adminOnlyEndpointReturns403ForProfessor() throws Exception {
        mockMvc.perform(get("/admin/exam-session"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void examDefinitionAdminPageReturns200ForAdmin() throws Exception {
        when(examDefinitionService.findAllPaged(anyInt(), anyInt(), any(Specification.class))).thenReturn(new PageImpl<>(List.of()));
        when(joinedSubjectService.findPage(anyInt(), anyInt(), any(Specification.class))).thenReturn(new PageImpl<>(List.of()));
        when(examDefinitionService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/exam-definition"))
                .andExpect(status().isOk())
                .andExpect(view().name("exam-definition"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void examDefinitionAdminPageReturns403ForStudent() throws Exception {
        mockMvc.perform(get("/admin/exam-definition"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void schedulingGenerateReturnsRedirectForAdmin() throws Exception {
        when(scheduleGenerationService.generateForSession("2025-26-JUNE")).thenReturn(List.of(new SubjectExam()));

        mockMvc.perform(post("/admin/scheduling/generate").param("yes", "2025-26-JUNE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/subject-exam?error=GenerateSuccessForSession:2025-26-JUNE(1)"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void professorSubmitEndpointReturnsForbiddenForAdmin() throws Exception {
        mockMvc.perform(post("/professor/subject-exam/SE1/submit").param("expectedStudents", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void professorSubmitEndpointReturnsForbiddenForStudent() throws Exception {
        mockMvc.perform(post("/professor/subject-exam/SE1/submit").param("expectedStudents", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicEndpointsAreAccessibleWithoutAuthentication() throws Exception {
        when(subjectExamService.findByDate(any())).thenReturn(List.of());
        when(roomService.findAllSortedByName()).thenReturn(List.of());
        when(subjectExamService.findOverlappingExamIds(any())).thenReturn(Set.of());
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.isProfessor()).thenReturn(false);

        mockMvc.perform(get("/admin/calendar-view/public").param("date", LocalDate.of(2026, 6, 10).toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("exams-calendar"));

        mockMvc.perform(get("/schedule"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/calendar-view/public"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void errorPage403ReturnsTemplate() throws Exception {
        mockMvc.perform(get("/error/403"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/403"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void postSaveSuccessAndInvalidInputAreHandled() throws Exception {
        when(yearExamSessionService.create(any(), any(), any(), any(), any(), any(), any())).thenReturn(new YearExamSession());

        mockMvc.perform(post("/admin/exam-session")
                        .param("session", "JUNE")
                        .param("year", "2025/26")
                        .param("sessionStart", "2026-06-10")
                        .param("sessionEnd", "2026-06-20")
                        .param("enrollmentStartDate", "2026-05-01")
                        .param("enrollmentEndDate", "2026-05-10")
                        .param("cycle", "UNDERGRADUATE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/exam-session"));

        mockMvc.perform(post("/admin/exam-session")
                        .param("session", "JUNE")
                        .param("year", "2025/26")
                        .param("sessionStart", "2026-06-10")
                        .param("sessionEnd", "2026-06-20")
                        .param("enrollmentStartDate", "2026-05-01")
                        .param("enrollmentEndDate", "2026-05-10"))
                .andExpect(status().isBadRequest());
    }
}

