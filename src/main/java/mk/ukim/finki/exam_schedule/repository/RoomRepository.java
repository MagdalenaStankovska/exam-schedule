package mk.ukim.finki.exam_schedule.repository;


import mk.ukim.finki.exam_schedule.model.Room;
import mk.ukim.finki.exam_schedule.model.RoomType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;


@Repository
public interface RoomRepository extends JpaSpecificationRepository<Room, String> {

    Set<Room> findAllByNameIn(Set<String> name);

    List<Room> findAllByType(RoomType type);

    List<Room> findAllByOrderByNameAsc();
}
