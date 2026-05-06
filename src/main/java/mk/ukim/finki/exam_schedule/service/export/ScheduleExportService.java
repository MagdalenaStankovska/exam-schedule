package mk.ukim.finki.exam_schedule.service.export;

public interface ScheduleExportService {
    byte[] exportCsv(String sessionName);

    byte[] exportXlsx(String sessionName);

    byte[] exportPdf(String sessionName);
}

