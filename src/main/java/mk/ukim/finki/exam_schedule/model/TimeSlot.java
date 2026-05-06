package mk.ukim.finki.exam_schedule.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDateTime fromTime;

    @NotNull
    private LocalDateTime toTime;

    public TimeSlot(LocalDateTime fromTime, LocalDateTime toTime) {
        this.fromTime = fromTime;
        this.toTime = toTime;
    }

    @AssertTrue(message = "Time slot duration must be at least 15 minutes and aligned to 15-minute intervals")
    public boolean isValidInterval() {
        if (fromTime == null || toTime == null || !toTime.isAfter(fromTime)) {
            return false;
        }
        long minutes = Duration.between(fromTime, toTime).toMinutes();
        return minutes >= 15 && minutes % 15 == 0;
    }

    public long getDurationMinutes() {
        if (fromTime == null || toTime == null) {
            return 0;
        }
        return Duration.between(fromTime, toTime).toMinutes();
    }
}

