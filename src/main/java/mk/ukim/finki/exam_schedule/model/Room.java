package mk.ukim.finki.exam_schedule.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.*;

@Getter
@Setter
@ToString
<<<<<<< HEAD
//@RequiredArgsConstructor
=======
>>>>>>> 11fd539188f35f2b8a5e569e419a7ee66f986eae
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Room {

    @Id
    private String name;

    private String locationDescription;

    private String equipmentDescription;

    @Enumerated
    private RoomType type;

    private Long capacity;
}
