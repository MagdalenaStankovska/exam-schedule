package mk.ukim.finki.exam_schedule.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TrackedHistoryEntity {

    @JsonIgnore
    private static transient ObjectMapper objectMapper = new ObjectMapper();

    static {
        // todo: uncomment next line in the target project
        // objectMapper.registerModule(new JSR310Module());
    }

    @JsonIgnore
    @ElementCollection
    private List<String> historyJson = new ArrayList<>();

    private LocalDateTime lastUpdateTime;

    private String lastUpdateUser;

    @PreUpdate
    @PrePersist
    public void addHistory() {
        this.lastUpdateTime = LocalDateTime.now();
        /* todo: uncomment this code in the target project
        if (SecurityContextHolder.getContext() != null &&
                SecurityContextHolder.getContext().getAuthentication() != null) {
            this.lastUpdateUser = SecurityContextHolder.getContext().getAuthentication().getName();
        }
         */
        try {
            String serializedState = objectMapper.writeValueAsString(this);
            historyJson.add(serializedState);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @JsonIgnore
    public List<? extends TrackedHistoryEntity> getHistory() {
        List<? extends TrackedHistoryEntity> result = this.historyJson.stream()
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, this.getClass());
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return result;
    }
}
