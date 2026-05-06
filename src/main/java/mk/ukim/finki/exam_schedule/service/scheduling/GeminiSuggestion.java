package mk.ukim.finki.exam_schedule.service.scheduling;

import java.time.LocalDateTime;
import java.util.Set;

public record GeminiSuggestion(String subjectExamId, LocalDateTime fromTime, LocalDateTime toTime, Set<String> roomNames) {
}

