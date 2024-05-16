package mk.ukim.finki.exam_schedule.service;


import mk.ukim.finki.exam_schedule.model.Room;

import java.util.List;

public interface RoomService {

    List<Room> findAll();
}
