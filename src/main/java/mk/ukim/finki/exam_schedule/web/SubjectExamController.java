package mk.ukim.finki.exam_schedule.web;

import mk.ukim.finki.exam_schedule.model.*;
import mk.ukim.finki.exam_schedule.service.ExamDefinitionService;
import mk.ukim.finki.exam_schedule.service.RoomService;
import mk.ukim.finki.exam_schedule.service.SubjectExamService;
import mk.ukim.finki.exam_schedule.service.YearExamSessionService;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static mk.ukim.finki.exam_schedule.service.specifications.FieldFilterSpecification.*;

@Controller
@RequestMapping("/admin/subject-exam")
public class SubjectExamController {

    private final SubjectExamService service;
    private final YearExamSessionService examSessionService;
    private final ExamDefinitionService examDefinitionService;
    private final RoomService roomService;

    public SubjectExamController(SubjectExamService service, YearExamSessionService examSessionService, ExamDefinitionService examDefinitionService, RoomService roomService) {
        this.service = service;
        this.examSessionService = examSessionService;
        this.examDefinitionService = examDefinitionService;
        this.roomService = roomService;
    }

    @GetMapping()
    public String listAll(Model model,
                          @RequestParam(required = false) String yes,
                          @RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "20") Integer results,
                          @RequestParam(required = false) String search,
                          @RequestParam(required = false) String room,
                          @RequestParam(required = false) String cycle){
        List<YearExamSession> yearExamSessions = this.examSessionService.listAll();
        StudyCycle cycle1 = cycle != null  && !cycle.isEmpty() ? StudyCycle.valueOf(cycle) : null;
        Specification<SubjectExam> filter1 = Specification.where((filterContainsText(SubjectExam.class, "definition.subject.name", search))).or(filterContainsText(SubjectExam.class,"id", search));

        Specification<SubjectExam> filter = Specification
                .where(filter1)
                .and(valueInList("rooms", room))
                .and(enumValueInList("session.cycle", cycle1));
        Page<SubjectExam> result = this.service.findAll(filter, pageNum, results);
        List<Room> rooms = this.roomService.findAll();
        model.addAttribute("page", result);
        model.addAttribute("cycles", StudyCycle.values());
        model.addAttribute("rooms", rooms);
        model.addAttribute("yearExamSessions", yearExamSessions);
        return "subjectExams";
    }

    @GetMapping("/initialize")
    public String initialize(Model model, @RequestParam String yes){
        List<ExamDefinition> examDefinitions = this.examDefinitionService.findAll();
        YearExamSession yearExamSession = this.examSessionService.findByName(yes);
        examDefinitions.stream().filter(e -> e.getExamSession() == yearExamSession.getSession()).forEach(e -> {this.service.create(yearExamSession, e);});
        return "redirect:/admin/subject-exam";
    }

    @GetMapping("/{name}/edit")
    public String showEdit(@PathVariable String name, Model model) {
        List<Room> rooms = this.roomService.findAll();
        model.addAttribute("se", service.findByName(name));
        model.addAttribute("rooms", rooms);
        return "editSubjectExam";
    }

    @PostMapping("/{name}/delete")
    public String delete(@PathVariable String name) {
        this.service.delete(name);
        return "redirect:/admin/subject-exam";
    }

    @PostMapping("/{name}")
    public String update(
            @PathVariable String name,
            @RequestParam YearExamSession session,
            @RequestParam Long durationMinutes,
            @RequestParam Long previousYearAttendantsNumber,
            @RequestParam Long previousYearTotalStudents,
            @RequestParam Long attendantsNumber,
            @RequestParam Long totalStudents,
            @RequestParam Long expectedNumber,
            @RequestParam Long numRepetitions,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toTime,
            @RequestParam Set<String> roomNames,
            @RequestParam String comment){
        this.service.update(name, session, durationMinutes, previousYearAttendantsNumber,
                previousYearTotalStudents, attendantsNumber, totalStudents, expectedNumber,
                numRepetitions, fromTime,  toTime, roomNames,  comment);
        return "redirect:/admin/subject-exam";
    }

}
