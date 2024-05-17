package mk.ukim.finki.exam_schedule.service;


import mk.ukim.finki.exam_schedule.model.Room;
import mk.ukim.finki.exam_schedule.model.RoomType;

import java.util.List;
import java.util.Set;

public interface RoomService {

    List<Room> findAll();

    List<Room> findAllByRoomType(RoomType type);

    Integer calculateTotalCapacityOfRooms(List<Room> rooms);

    Set<Room> findAllByNameIn(Set<String> roomNames);
}
