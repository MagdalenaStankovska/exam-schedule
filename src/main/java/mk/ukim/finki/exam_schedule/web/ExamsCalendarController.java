package mk.ukim.finki.exam_schedule.web;

import mk.ukim.finki.exam_schedule.model.*;
import mk.ukim.finki.exam_schedule.service.CurrentUserService;
import mk.ukim.finki.exam_schedule.service.RoomService;
import mk.ukim.finki.exam_schedule.service.SubjectExamService;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Controller
@RequestMapping("/admin/calendar-view")
public class ExamsCalendarController {
    private final SubjectExamService subjectExamService;
    private final RoomService roomService;
    private final CurrentUserService currentUserService;

    public ExamsCalendarController(SubjectExamService subjectExamService, RoomService roomService, CurrentUserService currentUserService) {
        this.subjectExamService = subjectExamService;
        this.roomService = roomService;
        this.currentUserService = currentUserService;
    }

    @GetMapping()
    public String calendarView(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                Model model) {
        if (date == null) {
            date = LocalDate.now();
        }
        List<SubjectExam> exams = subjectExamService.findByDate(date);
        List<Room> rooms = roomService.findAllSortedByName();
        Set<String> overlaps = subjectExamService.findOverlappingExamIds(exams);

        model.addAttribute("date", date);
        model.addAttribute("rooms", rooms);
        model.addAttribute("exams", exams);
        model.addAttribute("overlaps", overlaps);
        model.addAttribute("canReschedule", canReschedule());
        return "exams-calendar";
    }

    @GetMapping("/public")
    public String publicCalendar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 Model model) {
        String view = calendarView(date, model);
        model.addAttribute("canReschedule", false);
        return view;
    }

    private List<String> buildTimeLabels() {
        List<String> labels = new ArrayList<>();
        LocalTime time = LocalTime.of(8, 0);
        LocalTime end = LocalTime.of(21, 0);
        while (time.isBefore(end)) {
            labels.add(String.format("%02d:%02d", time.getHour(), time.getMinute()));
            time = time.plusMinutes(15);
        }
        return labels;
    }

    private boolean canReschedule() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean hasRole = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_PROFESSOR".equals(a.getAuthority()));
        return hasRole || currentUserService.isAdmin() || currentUserService.isProfessor();
    }
}