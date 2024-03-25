package mk.ukim.finki.exam_schedule.web;

import javassist.NotFoundException;
import mk.ukim.finki.exam_schedule.model.ExamDefinition;
import mk.ukim.finki.exam_schedule.model.ExamSession;
import mk.ukim.finki.exam_schedule.model.ExamType;
import mk.ukim.finki.exam_schedule.model.JoinedSubject;
import mk.ukim.finki.exam_schedule.service.ExamDefinitionService;
import mk.ukim.finki.exam_schedule.service.JoinedSubjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.List;

import static mk.ukim.finki.exam_schedule.service.specifications.FieldFilterSpecification.*;

@Controller
@RequestMapping("/admin/exam-definition")
public class ExamDefinitionController {

    private final ExamDefinitionService examDefinitionService;
    private final JoinedSubjectService joinedSubjectService;

    public ExamDefinitionController(ExamDefinitionService examDefinitionService, JoinedSubjectService joinedSubjectService) {
        this.examDefinitionService = examDefinitionService;
        this.joinedSubjectService = joinedSubjectService;
    }

    @GetMapping()
    public String list(Model model, @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "20") Integer results,
                       @RequestParam(defaultValue = "1") Integer subjectPageNum,
                       @RequestParam(defaultValue = "20") Integer subjectResults,
                       @RequestParam(required = false) String search,
                       @RequestParam(required = false) String examType,
                       @RequestParam(required = false) String note) {

        Page<ExamDefinition> pageExamDefintion = examDefinitionService.findAllPaged(pageNum, results, createExamDefinitionFilter(search, examType, note));

        Page<JoinedSubject> page = this.joinedSubjectService.findPage(subjectPageNum, subjectResults, createJoinedSubjectFilter(search));

        model.addAttribute("pageExamDefinition", pageExamDefintion);
        model.addAttribute("page", page);
        model.addAttribute("examTypes", ExamType.values());
        model.addAttribute("search", search);
        model.addAttribute("examType", examType);
        model.addAttribute("note", note);
        return "exam-definition";
    }

    @GetMapping("/{id}/edit")
    public String showEdit(@PathVariable String id, Model model) {
        ExamDefinition examDefinition = this.examDefinitionService.findById(id);
        model.addAttribute("definition", examDefinition);

        model.addAttribute("examTypes", ExamType.values());
        model.addAttribute("sessions", ExamSession.values());
        return "add-exam-definition";
    }

    @GetMapping("/{id}/add")
    public String showAdd(@PathVariable String id, Model model) {
        JoinedSubject joinedSubject = this.joinedSubjectService.findById(id);

        model.addAttribute("subject", joinedSubject);
        model.addAttribute("examTypes", ExamType.values());
        model.addAttribute("sessions", ExamSession.values());
        return "add-exam-definition";
    }

    @PostMapping("/{id}/delete")
    public String deleteExamDefinition(@PathVariable String id) {
        this.examDefinitionService.deleteById(id);
        return "redirect:/admin/exam-definition";
    }

    @PostMapping("/save")
    public String saveExamDefinition(@RequestParam() String subjectAbbreviation,
                                     @RequestParam() String examSession,
                                     @RequestParam() Long durationMinutes,
                                     @RequestParam() String type,
                                     @RequestParam() String note) throws NotFoundException {
        this.examDefinitionService.save(subjectAbbreviation, examSession, durationMinutes, type, note);
        return "redirect:/admin/exam-definition";
    }

    private Specification<ExamDefinition> createExamDefinitionFilter(String search, String examType, String note) {
        ExamType type = examType != null && !examType.isEmpty() ? ExamType.valueOf(examType) : null;

        return Specification.where(filterContainsTextCaseInsensitive(ExamDefinition.class, "note", note))
                .and(filterEnumEquals(ExamDefinition.class, "type", type))
                .and(filterContainsTextCaseInsensitive(ExamDefinition.class, "subject.name", search));
    }

    private Specification<JoinedSubject> createJoinedSubjectFilter(String search) {
        List<String> subjectAbbreviations = examDefinitionService.findAll()
                .stream()
                .map(definition -> definition.getSubject().getAbbreviation())
                .toList();

        return Specification.where(filterFieldNotInList(JoinedSubject.class, "abbreviation", subjectAbbreviations))
                .and(filterContainsTextCaseInsensitive(JoinedSubject.class, "name", search));
    }
}
