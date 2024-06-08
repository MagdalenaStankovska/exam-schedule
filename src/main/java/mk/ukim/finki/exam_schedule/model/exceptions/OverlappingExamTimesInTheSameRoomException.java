package mk.ukim.finki.exam_schedule.model.exceptions;

public class OverlappingExamTimesInTheSameRoomException extends RuntimeException {
    public OverlappingExamTimesInTheSameRoomException(String s) {
        System.out.println(s);
    }
}
