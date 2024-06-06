package mk.ukim.finki.exam_schedule.service.impl;

import javassist.NotFoundException;
import mk.ukim.finki.exam_schedule.model.ExamDefinition;
import mk.ukim.finki.exam_schedule.model.Room;
import mk.ukim.finki.exam_schedule.model.SubjectExam;
import mk.ukim.finki.exam_schedule.model.YearExamSession;
import mk.ukim.finki.exam_schedule.model.exceptions.InvalidYearExamSessionException;
import mk.ukim.finki.exam_schedule.model.exceptions.SubjectExamNotFoundException;
import mk.ukim.finki.exam_schedule.repository.ExamDefinitionRepository;
import mk.ukim.finki.exam_schedule.repository.RoomRepository;
import mk.ukim.finki.exam_schedule.repository.SubjectExamRepository;
import mk.ukim.finki.exam_schedule.repository.YearExamSessionRepository;
import mk.ukim.finki.exam_schedule.service.SubjectExamService;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SubjectExamServiceImpl implements SubjectExamService {

    private final SubjectExamRepository subjectExamRepository;
    private final YearExamSessionRepository yearExamSessionRepository;
    private final RoomRepository roomRepository;
    private final ExamDefinitionRepository examDefinitionRepository;

    public SubjectExamServiceImpl(SubjectExamRepository subjectExamRepository, YearExamSessionRepository yearExamSessionRepository, RoomRepository roomRepository, ExamDefinitionRepository examDefinitionRepository) {
        this.subjectExamRepository = subjectExamRepository;
        this.yearExamSessionRepository = yearExamSessionRepository;
        this.roomRepository = roomRepository;
        this.examDefinitionRepository = examDefinitionRepository;
    }

    @Override
    public SubjectExam findByName(String name) {
        return this.subjectExamRepository.findById(name).orElseThrow(SubjectExamNotFoundException::new);
    }

    @Override
    public Page<SubjectExam> findAll(Specification<SubjectExam> filter, Integer page, Integer size) {
        return this.subjectExamRepository.findAll(filter, PageRequest.of(page - 1, size,
                Sort.by(Sort.Direction.DESC, "session.year")
                        .and(Sort.by(Sort.Direction.DESC, "fromTime"))
                        .and(Sort.by(Sort.Direction.DESC, "toTime"))));
    }

    @Override
    public SubjectExam create(YearExamSession session, ExamDefinition definition) {
        return this.subjectExamRepository.save(new SubjectExam(definition, session));
    }

    @Override
    public void initialize(String yes) {
        List<ExamDefinition> examDefinitions = this.examDefinitionRepository.findAll();
        YearExamSession yearExamSession = this.yearExamSessionRepository.findById(yes).orElseThrow(InvalidYearExamSessionException::new);
        examDefinitions.stream().filter(e -> e.getExamSession() == yearExamSession.getSession())
                .forEach(e -> {
                    String id = String.format("%s-%s", yes, e.getId());
                    if (this.subjectExamRepository.findById(id).isEmpty()) {
                        this.create(yearExamSession, e);
                    }
                });

    }

    @Override
    public Set<Room> getAllRooms() {
        return new HashSet<>(roomRepository.findAll());
    }

    @Override
    public Set<Room> getRoomsByNames(Set<String> roomNames) {
        return new HashSet<>(roomRepository.findAllByNameIn(roomNames));
    }

    @Override
    public SubjectExam update(String name, YearExamSession session, Long durationMinutes,
                              Long previousYearAttendantsNumber, Long previousYearTotalStudents,
                              Long attendantsNumber, Long totalStudents, Long expectedNumber,
                              Long numRepetitions, LocalDateTime fromTime, LocalDateTime toTime,
                              Set<String> roomNames, String comment) {
        Set<Room> rooms = getRoomsByNames(roomNames);
        SubjectExam subjectExam = this.findByName(name);
        subjectExam.setSession(session);
        subjectExam.setDurationMinutes(durationMinutes);
        subjectExam.setPreviousYearAttendantsNumber(previousYearAttendantsNumber);
        subjectExam.setPreviousYearTotalStudents(previousYearTotalStudents);
        subjectExam.setAttendantsNumber(attendantsNumber);
        subjectExam.setTotalStudents(totalStudents);
        subjectExam.setExpectedNumber(expectedNumber);
        subjectExam.setNumRepetitions(numRepetitions);
        subjectExam.setFromTime(fromTime);
        subjectExam.setToTime(toTime);
        subjectExam.setRooms(rooms);
        subjectExam.setComment(comment);
        return this.subjectExamRepository.save(subjectExam);
    }

    @Override
    public SubjectExam delete(String name) {
        SubjectExam subjectExam = findByName(name);
        subjectExamRepository.delete(subjectExam);
        return subjectExam;
    }
}