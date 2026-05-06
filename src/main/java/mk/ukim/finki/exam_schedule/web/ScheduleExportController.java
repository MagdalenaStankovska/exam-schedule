package mk.ukim.finki.exam_schedule.web;

import mk.ukim.finki.exam_schedule.service.export.ScheduleExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/schedule-export")
public class ScheduleExportController {

    private final ScheduleExportService scheduleExportService;

    public ScheduleExportController(ScheduleExportService scheduleExportService) {
        this.scheduleExportService = scheduleExportService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{sessionName}")
    public ResponseEntity<byte[]> export(@PathVariable String sessionName,
                                         @RequestParam(defaultValue = "csv") String format) {
        String normalized = format.toLowerCase();
        byte[] content;
        MediaType mediaType;
        String extension;

        switch (normalized) {
            case "xlsx":
                content = scheduleExportService.exportXlsx(sessionName);
                mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                extension = "xlsx";
                break;
            case "pdf":
                content = scheduleExportService.exportPdf(sessionName);
                mediaType = MediaType.APPLICATION_PDF;
                extension = "pdf";
                break;
            default:
                content = scheduleExportService.exportCsv(sessionName);
                mediaType = MediaType.TEXT_PLAIN;
                extension = "csv";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("exam-schedule-" + sessionName + "." + extension)
                .build());
        return ResponseEntity.ok().headers(headers).body(content);
    }
}

