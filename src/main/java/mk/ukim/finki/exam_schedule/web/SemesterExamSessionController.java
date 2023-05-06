package mk.ukim.finki.exam_schedule.web;

import jakarta.websocket.Session;
import mk.ukim.finki.exam_schedule.model.ExamSession;
import mk.ukim.finki.exam_schedule.model.Semester;
import mk.ukim.finki.exam_schedule.model.SemesterExamSession;
import mk.ukim.finki.exam_schedule.service.SemesterExamSessionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class SemesterExamSessionController {

    private final SemesterExamSessionService service;

    public SemesterExamSessionController(SemesterExamSessionService service) {
        this.service = service;
    }

    @GetMapping(value = {"/", "/semesterexamsessions"})
    public String showList(@RequestParam(required = false) Semester semester,
                           @RequestParam(required = false) Model model) {
        List<SemesterExamSession> ses;
        if (semester == null) {
            ses = this.service.listAll();
        } else {
            ses = this.service.filter(semester);
        }

        model.addAttribute("semesterexamsession", ses);
        return "home";
    }

    @GetMapping("/semesterexamsessions/add")
    public String showAdd(Model model) {
        model.addAttribute("ses", new SemesterExamSession());
        return "home";
    }

    @GetMapping("/semesterexamsessions/{name}/edit")
    public String showEdit(@PathVariable String name, Model model) {
        model.addAttribute("emp", service.findByName(name));
        return "home";
    }

    @PostMapping("/semesterexamsessions/{name}/delete")
    public String delete(@PathVariable String name) {
        this.service.delete(name);
        return "redirect:/semesterexamsessions";
    }

    @PostMapping("/semesterexamsessions/{name}")
    public String update(
            @PathVariable String name,
            @RequestParam ExamSession session,
            @RequestParam Semester semester,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start, LocalDate end) {
        this.service.update(name, session, semester, start, end);
        return "redirect:/semesterexamsessions";
    }
}
