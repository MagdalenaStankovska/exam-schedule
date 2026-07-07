package mk.ukim.finki.exam_schedule.service;

import mk.ukim.finki.exam_schedule.model.Room;
import mk.ukim.finki.exam_schedule.model.RoomType;
import mk.ukim.finki.exam_schedule.repository.RoomRepository;
import mk.ukim.finki.exam_schedule.service.impl.RoomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock
    private RoomRepository repository;

    private RoomServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RoomServiceImpl(repository);
    }

    @Test
    void findAllReturnsRepositoryResult() {
        Room room = new Room("A1", "A", "projector", RoomType.CLASSROOM, 100L);
        when(repository.findAll()).thenReturn(List.of(room));

        assertThat(service.findAll()).containsExactly(room);
    }

    @Test
    void findAllByRoomTypeReturnsMatchingRooms() {
        Room room = new Room("L1", "Lab", "pcs", RoomType.LAB, 20L);
        when(repository.findAllByType(RoomType.LAB)).thenReturn(List.of(room));

        assertThat(service.findAllByRoomType(RoomType.LAB)).containsExactly(room);
    }

    @Test
    void calculateTotalCapacityOfRoomsSumsAllCapacitiesAndTreatsNullAsZeroByRepositoryInputContract() {
        Room first = new Room("A1", "A", "projector", RoomType.CLASSROOM, 100L);
        Room second = new Room("A2", "A", "projector", RoomType.CLASSROOM, 80L);

        assertThat(service.calculateTotalCapacityOfRooms(List.of(first, second))).isEqualTo(180);
    }

    @Test
    void findAllByNameInReturnsSetFromRepositoryList() {
        Room room = new Room("A1", "A", "projector", RoomType.CLASSROOM, 100L);
        when(repository.findAllByNameIn(Set.of("A1"))).thenReturn(Set.of(room));

        assertThat(service.findAllByNameIn(Set.of("A1"))).containsExactly(room);
    }

    @Test
    void findAllSortedByNameReturnsSortedRepositoryResult() {
        Room a = new Room("A1", "A", "", RoomType.CLASSROOM, 100L);
        Room b = new Room("B1", "B", "", RoomType.CLASSROOM, 100L);
        when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(a, b));

        assertThat(service.findAllSortedByName()).containsExactly(a, b);
    }
}


