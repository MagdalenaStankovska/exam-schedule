package mk.ukim.finki.exam_schedule.service.scheduling;

import mk.ukim.finki.exam_schedule.model.SubjectExam;

import java.util.List;

public interface GeminiScheduleClient {
    List<GeminiSuggestion> suggestSchedule(String sessionName, List<SubjectExam> exams);
}

