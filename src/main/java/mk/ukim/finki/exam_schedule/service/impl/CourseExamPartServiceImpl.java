//package mk.ukim.finki.exam_schedule.service.impl;
//
//import mk.ukim.finki.exam_schedule.model.Course;
//import mk.ukim.finki.exam_schedule.model.CourseExamPart;
//import mk.ukim.finki.exam_schedule.model.YearExamSession;
//import mk.ukim.finki.exam_schedule.model.dto.CourseExamPartDto;
//import mk.ukim.finki.exam_schedule.model.exceptions.CourseExamPartNotFoundException;
//import mk.ukim.finki.exam_schedule.repository.CourseExamPartRepository;
//import mk.ukim.finki.exam_schedule.service.CourseExamPartService;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Optional;
//
//@Service
//public class CourseExamPartServiceImpl implements CourseExamPartService {
//
//    private final CourseExamPartRepository courseExamPartRepository;
//
//    public CourseExamPartServiceImpl(CourseExamPartRepository courseExamPartRepository) {
//        this.courseExamPartRepository = courseExamPartRepository;
//    }
//
//
//    @Override
//    public List<CourseExamPart> listAll() {
//        return this.courseExamPartRepository.findAll();
//    }
//
//    @Override
//    public Optional<CourseExamPart> findById(String id) {
//        return this.courseExamPartRepository.findById(id);
//    }
//
//    @Override
//    public Optional<CourseExamPart> save(Course course, YearExamSession session, String name) {
//        CourseExamPart courseExamPart = new CourseExamPart(course, session, name);
//        this.courseExamPartRepository.save(courseExamPart);
//        return Optional.of(courseExamPart);
//    }
//
//    @Override
//    public Optional<CourseExamPart> save(CourseExamPartDto courseExamPartDto) {
//        CourseExamPart courseExamPart = new CourseExamPart(courseExamPartDto.getCourse(),
//                 courseExamPartDto.getSession(), courseExamPartDto.getName());
//        this.courseExamPartRepository.save(courseExamPart);
//        return Optional.of(courseExamPart);
//    }
//
//    @Override
//    public Optional<CourseExamPart> edit(String id, Course course, YearExamSession session, String name) {
//        CourseExamPart courseExamPart = this.courseExamPartRepository.findById(id).orElseThrow(() -> new CourseExamPartNotFoundException(id));
//        courseExamPart.setCourse(course);
//        courseExamPart.setSession(session);
//        courseExamPart.setName(name);
//        this.courseExamPartRepository.save(courseExamPart);
//        return Optional.of(courseExamPart);
//    }
//
//    @Override
//    public Optional<CourseExamPart> edit(String id, CourseExamPartDto courseExamPartDto) {
//        CourseExamPart courseExamPart = this.courseExamPartRepository.findById(id).orElseThrow(() -> new CourseExamPartNotFoundException(id));
//        courseExamPart.setCourse(courseExamPartDto.getCourse());
//        courseExamPart.setSession(courseExamPartDto.getSession());
//        courseExamPart.setName(courseExamPartDto.getName());
//        this.courseExamPartRepository.save(courseExamPart);
//        return Optional.of(courseExamPart);
//    }
//
//    @Override
//    public Optional<CourseExamPart> delete(String id) {
//        CourseExamPart courseExamPart = this.courseExamPartRepository.findById(id).orElseThrow(() -> new CourseExamPartNotFoundException(id));
//        this.courseExamPartRepository.delete(courseExamPart);
//        return Optional.of(courseExamPart);
//    }
//}
