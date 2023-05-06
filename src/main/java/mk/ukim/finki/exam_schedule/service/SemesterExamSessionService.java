package mk.ukim.finki.exam_schedule.service;

import mk.ukim.finki.exam_schedule.model.ExamSession;
import mk.ukim.finki.exam_schedule.model.Semester;
import mk.ukim.finki.exam_schedule.model.SemesterExamSession;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;


public interface SemesterExamSessionService {
    /**
     * @return List of all entities in the database
     */

    List<SemesterExamSession> listAll();

    /**
     * returns the entity with the given id
     *
     * @param name The id of the entity that we want to obtain
     * @return
     * @throws mk.ukim.finki.exam_schedule.model.exceptions.InvalidSemesterExamSessionIdException when there is no entity with the given id
     */
    SemesterExamSession findByName(String name);

    /**
     * This method is used to create a new entity, and save it in the database.
     *
     * @return The entity that is created. The id should be generated when the entity is created.
     * @throws mk.ukim.finki.exam_schedule.model.exceptions.InvalidSemesterExamSessionIdException when there is no category with the given id
     */
    SemesterExamSession create(String name, ExamSession session, Semester semester, LocalDate start, LocalDate end);

    /**
     * This method is used to modify an entity, and save it in the database.
     *
     * @param name          The id of the entity that is being edited
     * @return The entity that is updated.
     * @throws mk.ukim.finki.exam_schedule.model.exceptions.InvalidSemesterExamSessionIdException when there is no entity with the given id
     * @throws mk.ukim.finki.exam_schedule.model.exceptions.InvalidSemesterIdException    when there is no category with the given id
     */
    SemesterExamSession update(String name, ExamSession session, Semester semester, LocalDate start, LocalDate end);

    /**
     * Method that should delete an entity. If the id is invalid, it should throw InvalidEmployeeIdException.
     *
     * @param name
     * @return The entity that is deleted.
     * @throws mk.ukim.finki.exam_schedule.model.exceptions.InvalidSemesterExamSessionIdException when there is no entity with the given id
     */
    SemesterExamSession delete(String name);

    /**
     * The implementation of this method should use repository implementation for the filtering.
     * All arguments are nullable. When an argument is null, we should not filter by that attribute
     *
     * @return The entities that meet the filtering criteria
     */
    List<SemesterExamSession> filter(Semester semester);

}
