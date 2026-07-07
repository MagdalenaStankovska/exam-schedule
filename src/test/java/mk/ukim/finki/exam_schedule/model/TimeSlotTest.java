package mk.ukim.finki.exam_schedule.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TimeSlotTest {

    @Test
    void validIntervalWithThirtyMinutes() {
        TimeSlot slot = new TimeSlot(
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 10, 30)
        );

        assertThat(slot.isValidInterval()).isTrue();
        assertThat(slot.getDurationMinutes()).isEqualTo(30);
    }

    @Test
    void invalidIntervalWhenLessThanFifteenMinutes() {
        TimeSlot slot = new TimeSlot(
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 10, 10)
        );

        assertThat(slot.isValidInterval()).isFalse();
    }

    @Test
    void invalidIntervalWhenNotAlignedToFifteenMinutes() {
        TimeSlot slot = new TimeSlot(
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 10, 20)
        );

        assertThat(slot.isValidInterval()).isFalse();
    }

    @Test
    void zeroDurationIsInvalidAndHasZeroDurationMinutes() {
        TimeSlot slot = new TimeSlot(
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 10, 0)
        );

        assertThat(slot.isValidInterval()).isFalse();
        assertThat(slot.getDurationMinutes()).isZero();
    }

    @Test
    void negativeIntervalIsInvalidAndProducesNegativeDuration() {
        TimeSlot slot = new TimeSlot(
                LocalDateTime.of(2026, 4, 20, 10, 30),
                LocalDateTime.of(2026, 4, 20, 10, 0)
        );

        assertThat(slot.isValidInterval()).isFalse();
        assertThat(slot.getDurationMinutes()).isEqualTo(-30);
    }

    @Test
    void exactlyAtFifteenMinuteBoundaryIsValid() {
        TimeSlot slot = new TimeSlot(
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 10, 15)
        );

        assertThat(slot.isValidInterval()).isTrue();
        assertThat(slot.getDurationMinutes()).isEqualTo(15);
    }
}

