package mk.ukim.finki.exam_schedule.model.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class CourseExamPartNotFoundException extends RuntimeException{

    public CourseExamPartNotFoundException(String id) {
        System.out.println("CourseExamPart with" + id + " not found");
    }
}
