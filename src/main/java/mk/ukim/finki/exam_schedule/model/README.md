1. Create `SemesterExamSession` entity (CRUD page)
2. Session `CourseExamPart` preview and administration page by the professors (CRUD page)
    - Preview of all `CourseExamPart` entities for the current semester
    - For each Course in the semester create one CourseExamPart entity with name "all"
    - Import exam parts from csv
    - Filter by course name and semester and paging
    - Option to add new exam part by the professors  (new `CourseExamPart` button that will open an empty page for
      creating new `CourseExamPart` entity)
    - When click on an entity, separate page for editing the entity will be displayed where course and semester are not
      editable and the professor can add exam parts
3. Set rooms for the exam according to the needs for the part and set start/end times (at the end of the process)
   - To be defined in more details 