package mk.ukim.finki.exam_schedule.service.export.impl;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import mk.ukim.finki.exam_schedule.model.SubjectExam;
import mk.ukim.finki.exam_schedule.model.YearExamSession;
import mk.ukim.finki.exam_schedule.model.exceptions.InvalidYearExamSessionException;
import mk.ukim.finki.exam_schedule.repository.SubjectExamRepository;
import mk.ukim.finki.exam_schedule.repository.YearExamSessionRepository;
import mk.ukim.finki.exam_schedule.service.export.ScheduleExportService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScheduleExportServiceImpl implements ScheduleExportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SubjectExamRepository subjectExamRepository;
    private final YearExamSessionRepository yearExamSessionRepository;

    public ScheduleExportServiceImpl(SubjectExamRepository subjectExamRepository,
                                     YearExamSessionRepository yearExamSessionRepository) {
        this.subjectExamRepository = subjectExamRepository;
        this.yearExamSessionRepository = yearExamSessionRepository;
    }

    @Override
    public byte[] exportCsv(String sessionName) {
        StringBuilder sb = new StringBuilder("ExamId,Subject,From,To,Rooms,Expected,Type\n");
        for (SubjectExam exam : getOrderedExams(sessionName)) {
            sb.append(escape(exam.getId())).append(',')
                    .append(escape(exam.getDefinition().getSubject().getName())).append(',')
                    .append(escape(formatDate(exam.getFromTime()))).append(',')
                    .append(escape(formatDate(exam.getToTime()))).append(',')
                    .append(escape(exam.getRooms().stream().map(r -> r.getName()).collect(Collectors.joining("|")))).append(',')
                    .append(exam.getExpectedNumber() == null ? "" : exam.getExpectedNumber()).append(',')
                    .append(exam.getDefinition().getType())
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportXlsx(String sessionName) {
        // Fallback export that keeps build stable even when POI is unavailable.
        return exportCsv(sessionName);
    }

    @Override
    public byte[] exportPdf(String sessionName) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, bos);
            document.open();
            document.add(new Paragraph("Exam schedule: " + sessionName));
            document.add(new Paragraph(" "));
            for (SubjectExam exam : getOrderedExams(sessionName)) {
                document.add(new Paragraph(exam.getDefinition().getSubject().getName() + " | "
                        + formatDate(exam.getFromTime()) + " - " + formatDate(exam.getToTime())
                        + " | Rooms: " + exam.getRooms().stream().map(r -> r.getName()).collect(Collectors.joining(", "))));
            }
            document.close();
            return bos.toByteArray();
        } catch (IOException | DocumentException e) {
            throw new IllegalStateException("Failed to export PDF", e);
        }
    }

    private List<SubjectExam> getOrderedExams(String sessionName) {
        YearExamSession session = yearExamSessionRepository.findById(sessionName)
                .orElseThrow(InvalidYearExamSessionException::new);

        return subjectExamRepository.findAllBySession(session).stream()
                .sorted(Comparator.comparing(SubjectExam::getFromTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private String formatDate(java.time.LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATE_TIME_FORMATTER);
    }
}

