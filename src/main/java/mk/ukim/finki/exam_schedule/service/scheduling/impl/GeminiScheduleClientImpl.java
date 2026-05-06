package mk.ukim.finki.exam_schedule.service.scheduling.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mk.ukim.finki.exam_schedule.model.SubjectExam;
import mk.ukim.finki.exam_schedule.service.scheduling.GeminiScheduleClient;
import mk.ukim.finki.exam_schedule.service.scheduling.GeminiSuggestion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GeminiScheduleClientImpl implements GeminiScheduleClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent}")
    private String geminiUrl;

    @Override
    public List<GeminiSuggestion> suggestSchedule(String sessionName, List<SubjectExam> exams) {
        if (apiKey == null || apiKey.isBlank() || exams.isEmpty()) {
            return Collections.emptyList();
        }

        // Best-effort integration: even if Gemini is unavailable, scheduling falls back to deterministic logic.
        try {
            String prompt = buildPrompt(sessionName, exams);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = objectMapper.writeValueAsString(
                    Collections.singletonMap("contents", List.of(Collections.singletonMap("parts", List.of(Collections.singletonMap("text", prompt)))))
            );
            String response = restTemplate.postForEntity(geminiUrl + "?key=" + apiKey, new HttpEntity<>(body, headers), String.class)
                    .getBody();
            return parseSuggestions(response);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private String buildPrompt(String sessionName, List<SubjectExam> exams) {
        String examLines = exams.stream()
                .map(this::toExamLine)
                .collect(Collectors.joining("\\n"));
        return "You are an expert university exam scheduler. " +
                "Analyze the following exams and suggest only valid improvements for time and room allocation. " +
                "Constraints: no room/professor/student conflicts, capacity-safe room splitting for large groups, " +
                "LAB exams only in LAB rooms and CLASSROOM exams only in CLASSROOM rooms. " +
                "If an exam needs multiple rooms, keep same date/time for all rooms. " +
                "Output strictly one suggestion per line in this exact format: " +
                "SUGGESTION|<subjectExamId>|<fromTime ISO_LOCAL_DATE_TIME>|<toTime ISO_LOCAL_DATE_TIME>|<room1,room2,...>. " +
                "If no suggestion, output NO_SUGGESTIONS. " +
                "Session=" + sessionName + "\\nExams:\\n" + examLines;
    }

    private String toExamLine(SubjectExam exam) {
        String subjectId = exam.getDefinition() != null && exam.getDefinition().getSubject() != null
                ? exam.getDefinition().getSubject().getAbbreviation()
                : "UNKNOWN";
        String type = exam.getDefinition() != null && exam.getDefinition().getType() != null
                ? exam.getDefinition().getType().name()
                : "UNKNOWN";
        long expected = exam.getExpectedNumber() == null ? 0L : exam.getExpectedNumber();
        long duration = exam.getDurationMinutes() == null ? 60L : exam.getDurationMinutes();
        return exam.getId() + "|subject=" + subjectId + "|type=" + type + "|expected=" + expected + "|durationMin=" + duration;
    }

    private List<GeminiSuggestion> parseSuggestions(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return Collections.emptyList();
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            List<String> generatedTexts = new ArrayList<>();
            extractTextNodes(root, generatedTexts);
            if (generatedTexts.isEmpty()) {
                return Collections.emptyList();
            }

            List<GeminiSuggestion> suggestions = new ArrayList<>();
            for (String text : generatedTexts) {
                String[] lines = text.split("\\r?\\n");
                for (String line : lines) {
                    GeminiSuggestion parsed = parseSuggestionLine(line);
                    if (parsed != null) {
                        suggestions.add(parsed);
                    }
                }
            }
            return suggestions;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private void extractTextNodes(JsonNode node, List<String> output) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            JsonNode text = node.get("text");
            if (text != null && text.isTextual()) {
                output.add(text.asText());
            }
            node.fields().forEachRemaining(entry -> extractTextNodes(entry.getValue(), output));
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> extractTextNodes(child, output));
        }
    }

    private GeminiSuggestion parseSuggestionLine(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (!trimmed.startsWith("SUGGESTION|")) {
            return null;
        }

        String[] parts = trimmed.split("\\|", -1);
        if (parts.length < 5) {
            return null;
        }

        try {
            String examId = parts[1].trim();
            LocalDateTime from = LocalDateTime.parse(parts[2].trim());
            LocalDateTime to = LocalDateTime.parse(parts[3].trim());
            Set<String> roomNames = new LinkedHashSet<>();
            if (!parts[4].isBlank()) {
                for (String room : parts[4].split(",")) {
                    if (!room.isBlank()) {
                        roomNames.add(room.trim());
                    }
                }
            }
            return new GeminiSuggestion(examId, from, to, roomNames);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}

