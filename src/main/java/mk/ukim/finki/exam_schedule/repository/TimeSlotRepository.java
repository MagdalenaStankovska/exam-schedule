package mk.ukim.finki.exam_schedule.repository;

import mk.ukim.finki.exam_schedule.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
}

