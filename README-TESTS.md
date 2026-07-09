# Exam Schedule - Test Suite Documentation

## Team Contributions


| Member                  | Role | Contribution |
|-------------------------|------|--------------|
| **Marija** (Member 1)   | Web/MVC & Security Testing | Tests the Web/MVC + Security layer with MockMvc and spring-security-test — covering role-based authorization (ADMIN, PROFESSOR, STUDENT), public endpoints, and HTTP status codes. This fulfills the **mandatory bonus requirement** from the assignment. Created `WebMvcSecurityTest.java` (11 tests) and `HeaderUserAuthenticationFilterTest.java` (4 tests). |
| **Mihaela** (Member 2)  | Unit & Repository Testing | Extends existing unit testing and adds @DataJpaTest for the repository layer covering all 13 repositories with real H2 in-memory database + Flyway migrations (not mocks). Created `TimeSlotTest.java` (7 tests), `ModelBasicsTest.java` (8 tests), `RoomServiceImplTest.java` (5 tests), `JoinedSubjectServiceImplTest.java` (3 tests), `ExamDefinitionServiceImplTest.java` (6 tests), and `RepositoryLayerTest.java` (20 tests). |
| **Magdalena** (Member 3) | Integration & Export Verification | Created `ScheduleExportIntegrationTest.java` (5 tests) using real HTTP calls with `@SpringBootTest(webEnvironment = RANDOM_PORT)`. Verifies exported CSV/XLSX/PDF contents by parsing response bytes with Apache POI and PDFBox, plus role/anonymous security behavior for `/admin/schedule-export/**`. |

---

## Overview

This document describes the comprehensive test suite for the **exam-schedule** Spring Boot 3.1 application. The test suite covers all layers of the application: models, services, repositories, web controllers, security filters, and integration scenarios.

### Test Organization

Tests are organized into parallel sub-packages under `src/test/java/mk/ukim/finki/exam_schedule/`:

```
src/test/java/mk/ukim/finki/exam_schedule/
├── model/              # Unit tests for domain models
├── service/            # Unit tests for service layer (Mockito)
├── repository/         # Repository layer tests (@DataJpaTest, H2 + Flyway)
├── web/                # Web/MVC controller and security filter tests
├── integration/        # End-to-end integration tests (@SpringBootTest)
└── ui/                 # (Optional) Selenium UI tests
```

---

## Test Classes and Coverage

### 1. Model Tests (`src/test/java/mk/ukim/finki/exam_schedule/model/`)

#### **TimeSlotTest.java**
- **Purpose**: Validates TimeSlot domain model and time interval validation logic.
- **Coverage**:
    - Valid time slot creation and duration calculation
    - Invalid intervals (duration < 15 minutes, not divisible by 15)
    - Edge cases: zero duration, negative interval, exactly 15-minute boundary
    - `getDurationMinutes()` calculation
    - `isValidInterval()` validation method
- **Test Count**: 7 tests
- **Key Assertions**: AssertJ fluent assertions on duration and validity

#### **ModelBasicsTest.java**
- **Purpose**: Unit tests for model constructors and helper methods.
- **Coverage**:
    - **UserRole** enum: `isProfessor()`, `isStudent()`, `roleName()` helper methods
    - **YearExamSession** constructor: name derivation (e.g., "2025-26-JUNE"), submission deadline (3 weeks before start)
    - **SubjectExam** constructor: ID building (e.g., "2025-26-JUNE-ALG-JUNE-LAB"), workflow status initialization (DRAFT)
    - **SubjectAllocationStats** helpers and ID construction
- **Test Count**: 8 tests
- **Key Assertions**: Constructor side effects, enum method behavior

---

### 2. Service Layer Tests (`src/test/java/mk/ukim/finki/exam_schedule/service/`)

All service tests use `@ExtendWith(MockitoExtension.class)` and Mockito for dependency mocking.

#### **RoomServiceImplTest.java**
- **Purpose**: Tests RoomServiceImpl business logic.
- **Mocks**: `RoomRepository`
- **Coverage**:
    - `findAll()` — retrieves all rooms
    - `findAllByRoomType(RoomType)` — filters by room type
    - `calculateTotalCapacityOfRooms(Set<Room>)` — sums room capacities
    - `findAllByNameIn(Set<String>)` — retrieves rooms by name set
    - `findAllSortedByName()` — sorted room retrieval
- **Test Count**: 5 tests
- **Key Assertions**: Repository method calls, capacity calculations, sort order

#### **JoinedSubjectServiceImplTest.java**
- **Purpose**: Tests JoinedSubjectServiceImpl operations.
- **Mocks**: `JoinedSubjectRepository`
- **Coverage**:
    - `findById(UUID)` — successful retrieval and NoSuchElementException on miss
    - `findAll()` — retrieves all joined subjects
    - `findPage(Pageable, Specification<JoinedSubject>)` — pagination with filtering
- **Test Count**: 3 tests
- **Key Assertions**: Exception handling, pagination, Specification usage

#### **ExamDefinitionServiceImplTest.java**
- **Purpose**: Tests ExamDefinitionServiceImpl operations with cascading relationships.
- **Mocks**: `ExamDefinitionRepository`, `SubjectExamService`
- **Coverage**:
    - `findAll()` — retrieves all exam definitions
    - `findAllPaged(Pageable, Specification<ExamDefinition>)` — paged retrieval with filtering
    - `findById(UUID)` — retrieves single definition
    - `save(ExamDefinition)` — creates definition for all exam sessions
    - `edit(UUID, ExamDefinition)` — updates existing definition
    - `deleteById(UUID)` — cascades delete to all related SubjectExams
- **Test Count**: 6 tests
- **Key Assertions**: Repository interactions, cascading deletes, Specification filtering
- **Technical Notes**: Uses typed `ArgumentMatchers.<Specification<ExamDefinition>>any()` to avoid Mockito generic warnings

---

### 3. Repository Layer Test (`src/test/java/mk/ukim/finki/exam_schedule/repository/`)

#### **RepositoryLayerTest.java**
- **Purpose**: Comprehensive @DataJpaTest covering all 13 repository interfaces with real H2 database + Flyway migrations.
- **Test Database**: H2 in-memory (`jdbc:h2:mem:exam_schedule_test`), configured in `src/test/resources/application.properties`
- **Coverage**:

  **Flyway Migration Verification**:
    - Confirms all Flyway migrations execute successfully
    - Validates schema creation (tables exist)

  **13 Repository Query Methods**:
    1. **UserRepository**: `findByEmail(String)` — retrieves user by email
    2. **RoomRepository**:
        - `findAllByNameIn(Set<String>)` — retrieves rooms by name set
        - `findAllByType(RoomType)` — filters by room type
        - `findAllByOrderByNameAsc()` — sorted retrieval
    3. **JoinedSubjectRepository**: `findByAbbreviation(String)` — retrieves by abbreviation
    4. **ExamDefinitionRepository**: Custom Specification queries
    5. **YearExamSessionRepository**: Custom Specification queries
    6. **SubjectExamRepository**:
        - `findAllBySession(YearExamSession)` — retrieves all exams for session
        - `findAllByDefinitionAndSessionSession(ExamDefinition, YearExamSession)` — double filter
        - `findByDefinition_Subject()` — relationship traversal
        - `findBySessionCycle()` — cycle-based retrieval
        - `findByRoomsContaining(Room)` — room membership check
        - `findByIdWithRooms(UUID)` — eager load rooms
    7. **CourseRepository**: `findAllBySemester(Semester)` — semester filter
    8. **SemesterRepository**: `findById(UUID)` — basic retrieval
    9. **StudentCoursesRepository**: `findAllByCourse_JoinedSubject_AbbreviationIn(Set<String>)` — path traversal filter
    10. **SubjectAllocationStatsRepository**: `findAllBySubject(JoinedSubject)` — subject-based lookup
    11. **TeacherSubjectAllocationsRepository**:
        - `findAllByProfessorId(UUID)` — professor's allocations
        - `findAllByProfessorIdAndSubjectId(UUID, UUID)` — professor + subject filter
        - `findAllBySubjectIdIn(Set<UUID>)` — multi-subject lookup
    12. **TimeSlotRepository**: Verifies Flyway creates TimeSlot entities

- **Test Count**: ~20 assertions covering all repository methods
- **Key Assertions**: Entity persistence, custom query correctness, relationship integrity

---

### 4. Web/Security Tests (`src/test/java/mk/ukim/finki/exam_schedule/web/`)

#### **WebMvcSecurityTest.java**
- **Purpose**: Tests role-based authorization and web controller endpoints using @WebMvcTest + spring-security-test.
- **Configuration**: Imports `SecurityConfig` and `HeaderUserAuthenticationFilter`
- **Coverage**:

  **Role-Based Authorization (Admin-Only)**:
    - `/admin/exam-session` — returns 403 for PROFESSOR and STUDENT, 200 for ADMIN
    - `/admin/exam-definition` — returns 403 for non-admin, 200 for ADMIN
    - `/admin/scheduling/generate` — returns 403 for non-admin, 200 for ADMIN

  **Role-Based Authorization (Professor-Only)**:
    - `/professor/subject-exam/{id}/submit` — returns 403 for ADMIN and STUDENT

  **Public Endpoints**:
    - `/admin/calendar-view/public` — accessible without authentication
    - `/schedule` — public access
    - `/error/403` — public error page

  **POST Request Handling**:
    - POST success returns redirect
    - POST with missing required params returns 400 (Bad Request)

- **Test Count**: 11 tests
- **Key Assertions**: HTTP status codes (200, 403, 400, 302), role enforcement, redirect URIs
- **Technical Notes**: Uses `@WithMockUser(roles="ADMIN")` to set SecurityContext for role testing

#### **HeaderUserAuthenticationFilterTest.java**
- **Purpose**: Tests custom `HeaderUserAuthenticationFilter` which maps `X-User-Email` header to SecurityContext.
- **Coverage**:
    - User lookup from `UserRepository` via email from header
    - Authority population based on `UserRole` enum:
        - ADMIN-like roles (DEAN, ADMIN) → `ROLE_DEAN`, `ROLE_ADMIN`
        - PROFESSOR → `ROLE_PROFESSOR`
        - STUDENT → `ROLE_STUDENT`
    - Missing user/header handling
- **Test Count**: 4 tests
- **Key Assertions**: SecurityContext principal, authority names, header parsing
- **Technical Notes**: Direct filter test without full web context

---

### 5. Integration Tests (`src/test/java/mk/ukim/finki/exam_schedule/integration/`)

#### **ScheduleExportIntegrationTest.java**
- **Purpose**: End-to-end verification of schedule export through a real embedded HTTP server (not MockMvc).
- **Configuration**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` + H2 in-memory test database.
- **Coverage**:
    - `GET /admin/schedule-export/{sessionName}?format=xlsx` returns 200 and valid XLSX payload
    - Parses XLSX with Apache POI and validates header row + exported data row fields
    - `GET ...?format=pdf` returns 200 and valid PDF payload
    - Parses PDF with PDFBox and validates extracted text contains expected subject/room data
    - `GET ...?format=csv` returns 200 and CSV text containing expected subject/type
    - STUDENT role request returns exact 403 Forbidden
    - Anonymous request receives raw 3xx redirect to `/login` (redirect following disabled)
- **Test Count**: 5 tests
- **Key Assertions**: HTTP status and headers, binary payload parsing correctness, role-based security behavior
- **Technical Notes**: Uses `X-User-Email` header auth path from `HeaderUserAuthenticationFilter`.

---

### 6. UI Tests (Optional) (`src/test/java/mk/ukim/finki/exam_schedule/ui/`)

*(Optional, not yet created)*

**Future Selenium tests**:
- Login flow via header (X-User-Email injection via browser extension simulation or custom login page)
- Calendar view navigation
- Export button clicks and file downloads

---

## Running the Tests

### Prerequisites

- **Java 17+** installed and in PATH
- **Maven 3.8+** installed (or use bundled `mvnw.cmd`)
- **H2 Database** (in-memory, no setup required)
- **PostgreSQL** (for running production database separately, not needed for tests)

### Run All Tests

Using Maven from command line:

```bash
cd D:\IdeaProjects\exam-schedule
mvn clean test
```

Or using the bundled Maven wrapper:

```bash
mvnw clean test
```

**Expected Output**:
```
[INFO] Running mk.ukim.finki.exam_schedule.model.TimeSlotTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running mk.ukim.finki.exam_schedule.model.ModelBasicsTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running mk.ukim.finki.exam_schedule.service.RoomServiceImplTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running mk.ukim.finki.exam_schedule.service.JoinedSubjectServiceImplTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running mk.ukim.finki.exam_schedule.service.ExamDefinitionServiceImplTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running mk.ukim.finki.exam_schedule.repository.RepositoryLayerTest
[INFO] Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running mk.ukim.finki.exam_schedule.web.WebMvcSecurityTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running mk.ukim.finki.exam_schedule.config.HeaderUserAuthenticationFilterTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Total Tests**: ~69 tests

### Run Specific Test Class

```bash
mvn test -Dtest=TimeSlotTest
mvn test -Dtest=RoomServiceImplTest
mvn test -Dtest=WebMvcSecurityTest
```

### Run Tests with Coverage Report (JaCoCo)

Add JaCoCo plugin to `pom.xml` (or use IDE built-in coverage):

```bash
mvn clean test jacoco:report
```

Coverage report will be generated in `target/site/jacoco/index.html`.

### Run Tests in IDE (JetBrains IntelliJ IDEA)

1. Open the project in IntelliJ
2. Navigate to test file (e.g., `src/test/java/.../TimeSlotTest.java`)
3. Right-click on class name → **Run 'TimeSlotTest'**
4. Or right-click on method name → **Run 'testMethodName()'**
5. View results in "Run" panel at bottom

### Run Tests with Maven Debug

For debugging failing tests:

```bash
mvn test -X
```

This enables verbose logging to diagnose test failures.

---

## Test Database Configuration

### File: `src/test/resources/application.properties`

```properties
# H2 In-Memory Test Database
spring.datasource.url=jdbc:h2:mem:exam_schedule_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# Hibernate DDL & Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

# Flyway
spring.flyway.enabled=false
```

**Key Settings**:
- **MODE=PostgreSQL**: Allows H2 to emulate PostgreSQL syntax for compatibility
- **ddl-auto=update**: Allows Hibernate to create schema in H2 test runtime
- **flyway.enabled=false**: Flyway migrations are disabled for tests by current configuration

---

## Dependencies

### Test Dependencies (pom.xml)

```xml
<!-- Spring Security Test -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- H2 Database -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>

<!-- Mockito -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>

<!-- Apache POI (XLSX Export Verification) -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
</dependency>

<!-- PDFBox (PDF Export Verification) -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <scope>test</scope>
</dependency>

<!-- Spring Boot Test Starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Coverage Summary

| Layer | Test Class | Count | Status |
|-------|-----------|-------|--------|
| **Model** | TimeSlotTest | 7 | ✅ Complete |
|  | ModelBasicsTest | 8 | ✅ Complete |
| **Service** | RoomServiceImplTest | 5 | ✅ Complete |
|  | JoinedSubjectServiceImplTest | 3 | ✅ Complete |
|  | ExamDefinitionServiceImplTest | 6 | ✅ Complete |
| **Repository** | RepositoryLayerTest | 20 | ✅ Complete |
| **Web/Security** | WebMvcSecurityTest | 11 | ✅ Complete |
|  | HeaderUserAuthenticationFilterTest | 4 | ✅ Complete |
| **Integration** | ScheduleExportIntegrationTest | 5 | ✅ Complete |
| **UI (Optional)** | SeleniumUITest | — | 📋 Planned |
| **TOTAL** | — | **69+** | ✅ Core Complete |

---

## Common Issues and Troubleshooting

### Issue: Tests fail with "No hibernate property" or schema mismatch

**Solution**: Ensure `src/test/resources/application.properties` exists and contains H2 configuration. Spring Boot test profile should auto-load this file.

### Issue: Mockito generic warnings

**Solution**: Use typed `ArgumentMatchers.<Type>any()` instead of bare `any(Type.class)`. Already applied in service tests.

### Issue: "Connection refused" for PostgreSQL during test

**Solution**: Tests use H2 in-memory database, not PostgreSQL. If PostgreSQL is required, ensure `spring.datasource.url` in test config points to H2.

### Issue: Flyway migrations not running in tests

**Solution**: Current test configuration has `spring.flyway.enabled=false`, so this is expected. Enable Flyway in test properties only if migration-specific test coverage is required.

### Issue: Test database not cleaned between runs

**Solution**: Since Flyway is disabled in test runtime, use isolated fixtures per test and/or `@Transactional` rollback strategies.

---

## Future Enhancements

1. **UI Tests** (Selenium):
    - Browser automation for login and calendar view
    - File download verification

2. **Performance Tests**:
    - Load testing for schedule generation
    - Database query optimization tests

3. **Mutation Testing**:
    - Use PIT (Pitest) to verify test quality
    - Identify untested code paths

---

## References

- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [Spring Security Testing](https://spring.io/guides/gs/securing-web/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Apache POI XLSX](https://poi.apache.org/)
- [PDFBox User Guide](https://pdfbox.apache.org/)

---

**Last Updated**: July 6, 2026  
**Test Suite Version**: 1.0  
**Spring Boot Version**: 3.1  
**Java Version**: 17