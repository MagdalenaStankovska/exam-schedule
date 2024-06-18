package mk.ukim.finki.exam_schedule.web;

import mk.ukim.finki.exam_schedule.model.*;
import mk.ukim.finki.exam_schedule.service.ExamDefinitionService;
import mk.ukim.finki.exam_schedule.service.RoomService;
import mk.ukim.finki.exam_schedule.service.SubjectExamService;
import mk.ukim.finki.exam_schedule.service.YearExamSessionService;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static mk.ukim.finki.exam_schedule.service.specifications.FieldFilterSpecification.*;

@Controller
@RequestMapping("/admin/subject-exam")
public class SubjectExamController {

    private final SubjectExamService service;
    private final YearExamSessionService examSessionService;
    private final RoomService roomService;

    public SubjectExamController(SubjectExamService service, YearExamSessionService examSessionService, ExamDefinitionService examDefinitionService, RoomService roomService) {
        this.service = service;
        this.examSessionService = examSessionService;
        this.roomService = roomService;
    }

    @GetMapping()
    public String listAll(Model model,
                          @RequestParam(required = false) String yes,
                          @RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "15") Integer results,
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
        model.addAttribute("roomFilter", room);
        model.addAttribute("cycleFilter", cycle);
        model.addAttribute("search", search);
        model.addAttribute("yearExamSessions", yearExamSessions);
        return "subjectExams";
    }

    @GetMapping("/initialize")
    public String initialize(@RequestParam String yes){
        this.service.initialize(yes);
        return "redirect:/admin/subject-exam";
    }

    @PostMapping("/calculate")
    public String calculate(@RequestParam String yes){
        this.service.examCalculations(yes);
        return "redirect:/admin/subject-exam";
    }

    @GetMapping("/{name}/edit")
    public String showEdit(@PathVariable String name, Model model) {
        List<Room> rooms = this.roomService.findAll();
        model.addAttribute("se", service.findByName(name));
        model.addAttribute("sessions",examSessionService.listAll());
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
            @RequestParam (required = false) Long durationMinutes,
            @RequestParam (required = false) Long previousYearAttendantsNumber,
            @RequestParam (required = false) Long previousYearTotalStudents,
            @RequestParam (required = false) Long attendantsNumber,
            @RequestParam (required = false) Long totalStudents,
            @RequestParam (required = false) Long expectedNumber,
            @RequestParam (required = false) Long numRepetitions,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toTime,
            @RequestParam Set<String> roomNames,
            @RequestParam (required = false)String comment){
        this.service.update(name, session, durationMinutes, previousYearAttendantsNumber,
                previousYearTotalStudents, attendantsNumber, totalStudents, expectedNumber,
                numRepetitions, fromTime,  toTime, roomNames,  comment);
        return "redirect:/admin/subject-exam";
    }

    @PostMapping("/{id}/update-time")
    public ResponseEntity<String> updateTime(@PathVariable String id,
                                             @RequestBody Map<String, Object> requestBody) {
        if(!service.updateSubjectExamTime(id, (String) requestBody.get("fromTime"), (String) requestBody.get("toTime"))) return ResponseEntity.badRequest().build();

        return ResponseEntity.ok("Time updated successfully");
    }

    @PostMapping("{id}/update-repetitions")
    public ResponseEntity<String> updateRepetitions(@PathVariable String id,
                                                    @RequestBody Map<String, Object> requestBody) {

        service.updateSubjectExamNumRepetitions(id,Long.valueOf((String) requestBody.get("repetitions")));
        return ResponseEntity.ok("Repetitions updated successfully");
    }

    @PostMapping("{id}/recalculate")
    public String recalculate(@PathVariable String id) {
        service.recalculateSubjectExam(id);
        return "redirect:/admin/subject-exam";
    }


}
