package mk.ukim.finki.exam_schedule.service.impl;

import mk.ukim.finki.exam_schedule.model.ExamSession;
import mk.ukim.finki.exam_schedule.model.Semester;
import mk.ukim.finki.exam_schedule.model.SemesterExamSession;
import mk.ukim.finki.exam_schedule.model.exceptions.InvalidSemesterExamSessionIdException;
import mk.ukim.finki.exam_schedule.repository.SemesterExamSessionRepository;
import mk.ukim.finki.exam_schedule.service.SemesterExamSessionService;

import java.time.LocalDate;
import java.util.List;

public class SemesterExamSessionServiceImpl implements SemesterExamSessionService {

    private final SemesterExamSessionRepository semesterExamSessionRepository;

    public SemesterExamSessionServiceImpl(SemesterExamSessionRepository semesterExamSessionRepository){

        this.semesterExamSessionRepository = semesterExamSessionRepository;
    }

    @Override
    public List<SemesterExamSession> listAll() {
        return this.semesterExamSessionRepository.findAll();
    }

    @Override
    public SemesterExamSession findByName(String name) {
        return this.semesterExamSessionRepository.findById(name).orElseThrow(InvalidSemesterExamSessionIdException::new);
    }

    @Override
    public SemesterExamSession create(String name, ExamSession session, Semester semester, LocalDate start, LocalDate end) {
        return this.semesterExamSessionRepository.save(new SemesterExamSession(
                name,
                session,
                semester,
                start,
                end
        ));
    }

    @Override
    public SemesterExamSession update(String name, ExamSession session, Semester semester, LocalDate start, LocalDate end) {
        SemesterExamSession ses = this.findByName(name);
        ses.setName(name);
        ses.setSemester(semester);
        ses.setSession(session);
        ses.setStart(start);
        ses.setEnd(end);
        return this.semesterExamSessionRepository.save(ses);
    }

    @Override
    public SemesterExamSession delete(String name) {
        SemesterExamSession ses = findByName(name);
        this.semesterExamSessionRepository.delete(ses);
        return ses;
    }

    @Override
    public List<SemesterExamSession> filter(Semester semester) {
        if(semester!=null){
            return semesterExamSessionRepository.findBySemester(semester);
        }else {
            return semesterExamSessionRepository.findAll();
        }
    }
}
