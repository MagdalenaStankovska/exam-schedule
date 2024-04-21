package mk.ukim.finki.exam_schedule.service.impl;

import mk.ukim.finki.exam_schedule.model.ExamDefinition;
import mk.ukim.finki.exam_schedule.model.Room;
import mk.ukim.finki.exam_schedule.model.SubjectExam;
import mk.ukim.finki.exam_schedule.model.YearExamSession;
import mk.ukim.finki.exam_schedule.model.exceptions.SubjectExamNotFoundException;
import mk.ukim.finki.exam_schedule.repository.RoomRepository;
import mk.ukim.finki.exam_schedule.repository.SubjectExamRepository;
import mk.ukim.finki.exam_schedule.repository.YearExamSessionRepository;
import mk.ukim.finki.exam_schedule.service.SubjectExamService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SubjectExamServiceImpl implements SubjectExamService {

    private final SubjectExamRepository subjectExamRepository;
    private final YearExamSessionRepository yearExamSessionRepository;
    private final RoomRepository roomRepository;

    public SubjectExamServiceImpl(SubjectExamRepository subjectExamRepository, YearExamSessionRepository yearExamSessionRepository, RoomRepository roomRepository) {
        this.subjectExamRepository = subjectExamRepository;
        this.yearExamSessionRepository = yearExamSessionRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    public SubjectExam findByName(String name) {
        return this.subjectExamRepository.findById(name).orElseThrow(SubjectExamNotFoundException::new);
    }

    @Override
    public Page<SubjectExam> findAll(Specification<SubjectExam> filter, Integer page, Integer size) {
        return this.subjectExamRepository.findAll(filter, PageRequest.of(page - 1, size));
    }

    @Override
    public SubjectExam create(YearExamSession session, ExamDefinition definition) {
        return this.subjectExamRepository.save(new SubjectExam(definition, session));
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

    @Override
    public SubjectExam save(SubjectExam exam) {
        return subjectExamRepository.save(exam);
    }
}