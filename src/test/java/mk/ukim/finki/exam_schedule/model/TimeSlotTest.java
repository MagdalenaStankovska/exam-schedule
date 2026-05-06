package mk.ukim.finki.exam_schedule.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class TimeSlotTest {

    @Test
    void validIntervalWithThirtyMinutes() {
        TimeSlot slot = new TimeSlot(
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 10, 30)
        );

        Assertions.assertTrue(slot.isValidInterval());
        Assertions.assertEquals(30, slot.getDurationMinutes());
    }

    @Test
    void invalidIntervalWhenLessThanFifteenMinutes() {
        TimeSlot slot = new TimeSlot(
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 10, 10)
        );

        Assertions.assertFalse(slot.isValidInterval());
    }

    @Test
    void invalidIntervalWhenNotAlignedToFifteenMinutes() {
        TimeSlot slot = new TimeSlot(
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 10, 20)
        );

        Assertions.assertFalse(slot.isValidInterval());
    }
}

