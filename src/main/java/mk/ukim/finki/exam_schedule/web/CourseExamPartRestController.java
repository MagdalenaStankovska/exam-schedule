package mk.ukim.finki.exam_schedule.web;

import mk.ukim.finki.exam_schedule.model.CourseExamPart;
import mk.ukim.finki.exam_schedule.model.dto.CourseExamPartDto;
import mk.ukim.finki.exam_schedule.service.CourseExamPartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin(origins = {"http://localhost:3000","http://localhost:3001"})
@RequestMapping("/api/exams")
public class CourseExamPartRestController {

    private final CourseExamPartService courseExamPartService;

    public CourseExamPartRestController(CourseExamPartService courseExamPartService) {
        this.courseExamPartService = courseExamPartService;
    }

    // LIST ALL
    @GetMapping
    public List<CourseExamPart> findAll(){
        return this.courseExamPartService.listAll();
    }

    // GET BY ID
    @GetMapping("/{id}")
    public Optional<CourseExamPart> findById(@PathVariable String id){
        return this.courseExamPartService.findById(id);
    }

    // ADD
    @PostMapping("/add")
    public ResponseEntity<CourseExamPart> save(@RequestBody CourseExamPartDto courseExamPartDto){
        return this.courseExamPartService.save(courseExamPartDto)
                .map(courseExamPart -> ResponseEntity.ok().body(courseExamPart))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    // EDIT
    @PutMapping("/edit/{id}")
    public ResponseEntity<CourseExamPart> edit(@PathVariable String id, @RequestBody CourseExamPartDto courseExamPartDto){
        return this.courseExamPartService.edit(id, courseExamPartDto)
                .map(courseExamPart -> ResponseEntity.ok().body(courseExamPart))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    // DELETE
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<CourseExamPart> deleteById(@PathVariable String id){
        this.courseExamPartService.delete(id);
        if (this.courseExamPartService.findById(id).isEmpty()) return ResponseEntity.ok().build();
        return ResponseEntity.badRequest().build();
    }

}
