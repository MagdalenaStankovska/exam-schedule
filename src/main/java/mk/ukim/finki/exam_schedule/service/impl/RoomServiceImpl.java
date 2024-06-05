package mk.ukim.finki.exam_schedule.service.impl;

import mk.ukim.finki.exam_schedule.model.Room;
import mk.ukim.finki.exam_schedule.model.RoomType;
import mk.ukim.finki.exam_schedule.repository.RoomRepository;
import mk.ukim.finki.exam_schedule.service.RoomService;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoomServiceImpl implements RoomService {

    private final RoomRepository repository;

    public RoomServiceImpl(RoomRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Room> findAll() {
        return this.repository.findAll();
    }

    @Override
    public List<Room> findAllByRoomType(RoomType type) {
        return this.repository.findAllByType(type);
    }

    @Override
    public Integer calculateTotalCapacityOfRooms(List<Room> rooms) {
       return rooms.stream().mapToInt(room -> Math.toIntExact(room.getCapacity())).sum();
    }

    @Override
    public Set<Room> findAllByNameIn(Set<String> roomNames) {
        return new HashSet<>(this.repository.findAllByNameIn(roomNames));
    }
}

