package mk.ukim.finki.exam_schedule.web;

import mk.ukim.finki.exam_schedule.service.SubjectExamService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/professor/subject-exam")
public class ProfessorSubjectExamController {

    private final SubjectExamService subjectExamService;

    public ProfessorSubjectExamController(SubjectExamService subjectExamService) {
        this.subjectExamService = subjectExamService;
    }

    @PreAuthorize("hasRole('PROFESSOR')")
    @PostMapping("/{id}/submit")
    public String submitExpectedStudents(@PathVariable String id,
                                         @RequestParam Long expectedStudents) {
        boolean submitted = subjectExamService.submitExpectedStudents(id, expectedStudents);
        if (!submitted) {
            return "redirect:/admin/subject-exam?error=ExpectedNumberDeadlineViolation";
        }
        return "redirect:/admin/subject-exam";
    }
}


