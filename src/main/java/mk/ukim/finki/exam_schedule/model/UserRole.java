package mk.ukim.finki.exam_schedule.model;

public enum UserRole {
    STUDENT(false, true),
    // professors
    PROFESSOR(true, false),
    ACADEMIC_AFFAIR_VICE_DEAN(true, false),
    SCIENCE_AND_COOPERATION_VICE_DEAN(true, false),
    FINANCES_VICE_DEAN(true, false),
    DEAN(true, false),
    // staff
    STUDENT_ADMINISTRATION(false, false),
    STUDENT_ADMINISTRATION_MANAGER(false, false),
    FINANCE_ADMINISTRATION(false, false),
    FINANCE_ADMINISTRATION_MANAGER(false, false),
    LEGAL_ADMINISTRATION(false, false),
    ARCHIVE_ADMINISTRATION(false, false),
    ADMINISTRATION_MANAGER(false, false),
    // external professors
    EXTERNAL(true, false);

    private final Boolean professor;

    private final Boolean student;

    UserRole(Boolean professor, Boolean student) {
        this.professor = professor;
        this.student = student;
    }

    public Boolean isProfessor() {
        return professor;
    }

    public Boolean isStudent() {
        return student;
    }

    public String roleName() {
        return "ROLE_" + this.name();
    }
}

