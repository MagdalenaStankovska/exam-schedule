package mk.ukim.finki.exam_schedule.integration;

import mk.ukim.finki.exam_schedule.model.ExamDefinition;
import mk.ukim.finki.exam_schedule.model.ExamSession;
import mk.ukim.finki.exam_schedule.model.ExamType;
import mk.ukim.finki.exam_schedule.model.ExamWorkflowStatus;
import mk.ukim.finki.exam_schedule.model.JoinedSubject;
import mk.ukim.finki.exam_schedule.model.Room;
import mk.ukim.finki.exam_schedule.model.RoomType;
import mk.ukim.finki.exam_schedule.model.SemesterType;
import mk.ukim.finki.exam_schedule.model.StudyCycle;
import mk.ukim.finki.exam_schedule.model.SubjectExam;
import mk.ukim.finki.exam_schedule.model.User;
import mk.ukim.finki.exam_schedule.model.UserRole;
import mk.ukim.finki.exam_schedule.model.YearExamSession;
import mk.ukim.finki.exam_schedule.repository.ExamDefinitionRepository;
import mk.ukim.finki.exam_schedule.repository.JoinedSubjectRepository;
import mk.ukim.finki.exam_schedule.repository.RoomRepository;
import mk.ukim.finki.exam_schedule.repository.SubjectExamRepository;
import mk.ukim.finki.exam_schedule.repository.UserRepository;
import mk.ukim.finki.exam_schedule.repository.YearExamSessionRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ScheduleExportIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private YearExamSessionRepository yearExamSessionRepository;

    @Autowired
    private JoinedSubjectRepository joinedSubjectRepository;

    @Autowired
    private ExamDefinitionRepository examDefinitionRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SubjectExamRepository subjectExamRepository;

    private String sessionName;
    private String subjectName;
    private String roomName;
    private String adminEmail;
    private String studentEmail;
    private String examType;

    @BeforeEach
    void setUp() {
        subjectExamRepository.deleteAll();
        examDefinitionRepository.deleteAll();
        yearExamSessionRepository.deleteAll();
        joinedSubjectRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();

        JoinedSubject subject = new JoinedSubject();
        subject.setAbbreviation("ALG");
        subject.setName("Algorithms");
        subject.setSemesterType(SemesterType.WINTER);
        subject = joinedSubjectRepository.save(subject);

        YearExamSession session = yearExamSessionRepository.save(new YearExamSession(
                ExamSession.JUNE,
                "2027/28",
                LocalDate.of(2028, 6, 10),
                LocalDate.of(2028, 6, 20),
                LocalDate.of(2028, 5, 1),
                LocalDate.of(2028, 5, 10),
                List.of(StudyCycle.UNDERGRADUATE)
        ));

        ExamDefinition definition = examDefinitionRepository.save(
                new ExamDefinition("ALG-JUNE-CLASSROOM", subject, ExamSession.JUNE, 90L, ExamType.CLASSROOM, "core")
        );

        Room room = roomRepository.save(new Room("A1", "Main", "projector", RoomType.CLASSROOM, 100L));

        SubjectExam exam = new SubjectExam(definition, session);
        exam.setFromTime(LocalDateTime.of(2028, 6, 11, 10, 0));
        exam.setToTime(LocalDateTime.of(2028, 6, 11, 11, 30));
        exam.setRooms(Set.of(room));
        exam.setWorkflowStatus(ExamWorkflowStatus.SCHEDULED);
        exam.setExpectedNumber(85L);
        subjectExamRepository.save(exam);

        userRepository.save(new User("admin-user", "Admin Caller", "admin.export@example.com", UserRole.ADMINISTRATION_MANAGER));
        userRepository.save(new User("student-user", "Student Caller", "student.export@example.com", UserRole.STUDENT));

        sessionName = session.getName();
        subjectName = subject.getName();
        roomName = room.getName();
        adminEmail = "admin.export@example.com";
        studentEmail = "student.export@example.com";
        examType = definition.getType().name();
    }

    @Test
    void exportXlsx_returnsWorkbookWithCorrectData() throws IOException {
        ResponseEntity<byte[]> response = testRestTemplate.exchange(
                "/admin/schedule-export/{sessionName}?format=xlsx",
                HttpMethod.GET,
                authEntity(adminEmail),
                byte[].class,
                sessionName
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().toString()).contains("spreadsheetml");
        assertThat(response.getBody()).isNotNull();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
            var sheet = workbook.getSheetAt(0);
            var header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("ExamId");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("Subject");
            assertThat(header.getCell(2).getStringCellValue()).isEqualTo("From");
            assertThat(header.getCell(3).getStringCellValue()).isEqualTo("To");
            assertThat(header.getCell(4).getStringCellValue()).isEqualTo("Rooms");
            assertThat(header.getCell(5).getStringCellValue()).isEqualTo("Expected");
            assertThat(header.getCell(6).getStringCellValue()).isEqualTo("Type");

            var row = sheet.getRow(1);
            assertThat(row.getCell(1).getStringCellValue()).isEqualTo(subjectName);
            assertThat(row.getCell(4).getStringCellValue()).contains(roomName);
            assertThat(row.getCell(6).getStringCellValue()).isEqualTo(examType);
        }
    }

    @Test
    void exportPdf_containsExpectedText() throws IOException {
        ResponseEntity<byte[]> response = testRestTemplate.exchange(
                "/admin/schedule-export/{sessionName}?format=pdf",
                HttpMethod.GET,
                authEntity(adminEmail),
                byte[].class,
                sessionName
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().toString()).contains("application/pdf");
        assertThat(response.getBody()).isNotNull();

        try (PDDocument document = PDDocument.load(response.getBody())) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains(subjectName);
            assertThat(text).contains(roomName);
        }
    }

    @Test
    void exportCsv_containsExpectedRow() {
        ResponseEntity<byte[]> response = testRestTemplate.exchange(
                "/admin/schedule-export/{sessionName}?format=csv",
                HttpMethod.GET,
                authEntity(adminEmail),
                byte[].class,
                sessionName
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        String csv = new String(response.getBody(), StandardCharsets.UTF_8);
        assertThat(csv).contains(subjectName);
        assertThat(csv).contains(examType);
    }

    @Test
    void studentRoleGetsForbidden() {
        ResponseEntity<byte[]> response = testRestTemplate.exchange(
                "/admin/schedule-export/{sessionName}?format=xlsx",
                HttpMethod.GET,
                authEntity(studentEmail),
                byte[].class,
                sessionName
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void anonymousRequestIsRedirectedToLogin() {
        RestTemplate restTemplate = new RestTemplate(new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        });

        URI uri = URI.create("http://localhost:" + port + "/admin/schedule-export/" + sessionName + "?format=xlsx");
        ResponseEntity<byte[]> response = restTemplate.exchange(uri, HttpMethod.GET, HttpEntity.EMPTY, byte[].class);

        assertThat(response.getStatusCode().is3xxRedirection()).isTrue();
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getHeaders().getLocation().toString()).contains("/login");
    }

    private HttpEntity<Void> authEntity(String email) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Email", email);
        return new HttpEntity<>(headers);
    }
}