package mk.ukim.finki.exam_schedule.web;

import mk.ukim.finki.exam_schedule.model.*;
import mk.ukim.finki.exam_schedule.service.RoomService;
import mk.ukim.finki.exam_schedule.service.SubjectExamService;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/admin/calendar-view")
public class ExamsCalendarController {
    private final SubjectExamService subjectExamService;
    private final RoomService roomService;

    public ExamsCalendarController(SubjectExamService subjectExamService, RoomService roomService) {
        this.subjectExamService = subjectExamService;
        this.roomService = roomService;
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
        return "exams-calendar";
    }
}