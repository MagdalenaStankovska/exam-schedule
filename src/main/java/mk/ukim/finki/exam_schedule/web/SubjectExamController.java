package mk.ukim.finki.exam_schedule.web;

import mk.ukim.finki.exam_schedule.model.*;
import mk.ukim.finki.exam_schedule.service.CurrentUserService;
import mk.ukim.finki.exam_schedule.service.RoomService;
import mk.ukim.finki.exam_schedule.service.SubjectExamService;
import mk.ukim.finki.exam_schedule.service.YearExamSessionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final CurrentUserService currentUserService;

    public SubjectExamController(SubjectExamService service, YearExamSessionService examSessionService, RoomService roomService, CurrentUserService currentUserService) {
        this.service = service;
        this.examSessionService = examSessionService;
        this.roomService = roomService;
        this.currentUserService = currentUserService;
    }

    @GetMapping()
    public String listAll(Model model,
                          @RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "15") Integer results,
                          @RequestParam(required = false)  String error,
                          @RequestParam(required = false) String search,
                          @RequestParam(required = false) String room,
                          @RequestParam(required = false) String cycle) {
        List<YearExamSession> yearExamSessions = this.examSessionService.listAll();
        StudyCycle cycle1 = cycle != null && !cycle.isEmpty() ? StudyCycle.valueOf(cycle) : null;
        Specification<SubjectExam> filter1 = Specification.where((filterContainsText(SubjectExam.class, "definition.subject.name", search))).or(filterContainsText(SubjectExam.class, "id", search));

        Specification<SubjectExam> filter = Specification
                .where(filter1)
                .and(valueInList("rooms", room))
                .and(enumValueInList("session.cycle", cycle1));
        Page<SubjectExam> result = this.service.findAll(filter, pageNum, results);
        List<Room> rooms = this.roomService.findAll();
        String decodedError = error == null ? null : URLDecoder.decode(error, StandardCharsets.UTF_8);
        model.addAttribute("page", result);
        model.addAttribute("cycles", StudyCycle.values());
        model.addAttribute("rooms", rooms);
        model.addAttribute("roomFilter", room);
        model.addAttribute("cycleFilter", cycle);
        model.addAttribute("error", decodedError);
        model.addAttribute("search", search);
        model.addAttribute("yearExamSessions", yearExamSessions);
        model.addAttribute("isAdmin", currentUserService.isAdmin());
        model.addAttribute("isProfessor", currentUserService.isProfessor());
        return "subjectExams";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/initialize")
    public String initialize(@RequestParam String yes) {
        try {
            this.service.initialize(yes);
            return "redirect:/admin/subject-exam";
        } catch (Exception ex) {
            String reason = ex.getMessage() == null || ex.getMessage().isBlank()
                    ? ex.getClass().getSimpleName()
                    : ex.getMessage();
            return "redirect:/admin/subject-exam?error=InitializeFailedForSession:" + yes + ":" + URLEncoder.encode(reason, StandardCharsets.UTF_8);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/calculate")
    public String calculate(@RequestParam String yes) {
        try {
            this.service.examCalculations(yes);
            return "redirect:/admin/subject-exam";
        } catch (Exception ex) {
            String reason = ex.getMessage() == null || ex.getMessage().isBlank()
                    ? ex.getClass().getSimpleName()
                    : ex.getMessage();
            return "redirect:/admin/subject-exam?error=CalculateFailedForSession:" + yes + ":" + URLEncoder.encode(reason, StandardCharsets.UTF_8);
        }
    }

    @GetMapping("/{name}/edit")
    public String showEdit(@PathVariable String name, Model model) {
        SubjectExam se = service.findByName(name);
        List<Room> rooms = service.getRoomsByType(name);
        model.addAttribute("se", service.CheckPreviousYear(name, se.getDefinition(), se.getSession()));
        model.addAttribute("sessions", examSessionService.listAll());
        model.addAttribute("rooms", rooms);
        model.addAttribute("examRooms",  se.getRooms());
        model.addAttribute("showRooms", service.needsRooms(name));
        return "editSubjectExam";
    }

    @PostMapping("/{name}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable String name) {
        this.service.delete(name);
        return "redirect:/admin/subject-exam";
    }

    @PostMapping("/{name}")
    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR')")
    public String update(
            @PathVariable String name,
            @RequestParam YearExamSession session,
            @RequestParam(required = false) Long durationMinutes,
            @RequestParam(required = false) Long previousYearAttendantsNumber,
            @RequestParam(required = false) Long previousYearTotalStudents,
            @RequestParam(required = false) Long attendantsNumber,
            @RequestParam(required = false) Long totalStudents,
            @RequestParam(required = false) Long expectedNumber,
            @RequestParam(required = false) Long numRepetitions,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toTime,
            @RequestParam(required = false) Set<String> roomNames,
            @RequestParam(required = false) String comment) {
        if(this.service.update(name, session, durationMinutes, previousYearAttendantsNumber,
                previousYearTotalStudents, attendantsNumber, totalStudents, expectedNumber,
                numRepetitions, fromTime, toTime, roomNames, comment) != null) {
            return "redirect:/admin/subject-exam";
        }
        return "redirect:/admin/subject-exam?error=InvalidDateTime";
    }

    @PostMapping("/{id}/update-time")
    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR')")
    public ResponseEntity<String> updateTime(@PathVariable String id,
                                             @RequestBody Map<String, Object> requestBody) {
        String fromTime = (String) requestBody.get("fromTime");
        String toTime = (String) requestBody.get("toTime");
        String roomName = requestBody.get("roomName") == null ? null : String.valueOf(requestBody.get("roomName"));

        if (fromTime == null || toTime == null) {
            return ResponseEntity.badRequest().body("Missing fromTime/toTime payload.");
        }
        if (service.checkInvalidDateTimeInput(fromTime, toTime)) {
            return ResponseEntity.badRequest().body("Invalid time range. Use 15-minute aligned intervals.");
        }
        if (!service.updateSubjectExamPlacement(id, fromTime, toTime, roomName)) {
            return ResponseEntity.badRequest().body("Unable to move exam. Check permissions, overlap, room type, and room capacity.");
        }

        return ResponseEntity.ok("Time updated successfully");
    }

    @PostMapping("{id}/update-repetitions")
    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR')")
    public ResponseEntity<String> updateRepetitions(@PathVariable String id,
                                                    @RequestBody Map<String, Object> requestBody) {

        Object repsObj = requestBody.get("repetitions");
        long repetitions;
        if (repsObj instanceof Number) {
            repetitions = ((Number) repsObj).longValue();
        } else if (repsObj instanceof String) {
            repetitions = Long.parseLong((String) repsObj);
        } else {
            return ResponseEntity.badRequest().body("Invalid repetitions value");
        }

        service.updateSubjectExamNumRepetitions(id, repetitions);
        return ResponseEntity.ok("Repetitions updated successfully");
    }

    @PostMapping("{id}/recalculate")
    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR')")
    public String recalculate(@PathVariable String id) {
        service.recalculateSubjectExam(id);
        return "redirect:/admin/subject-exam";
    }

    @GetMapping("/{seId}/{roomName}")
    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR')")
    public String removeRoomFromExam(@PathVariable String seId,
                                     @PathVariable String roomName) {
        service.removeRoom(seId, roomName);
        return "redirect:/admin/subject-exam";
    }
}
