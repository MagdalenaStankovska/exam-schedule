package mk.ukim.finki.exam_schedule.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class PublicScheduleController {

    @GetMapping("/schedule")
    public String schedule(@RequestParam(required = false) LocalDate date) {
        if (date == null) {
            return "redirect:/admin/calendar-view/public";
        }
        return "redirect:/admin/calendar-view/public?date=" + date;
    }
}

