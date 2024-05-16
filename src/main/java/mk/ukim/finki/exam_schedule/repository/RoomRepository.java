package mk.ukim.finki.exam_schedule.repository;


import mk.ukim.finki.exam_schedule.model.Room;
import org.springframework.stereotype.Repository;

import java.util.Set;


@Repository
public interface RoomRepository extends JpaSpecificationRepository<Room, String> {

    Set<Room> findAllByNameIn(Set<String> name);
}
