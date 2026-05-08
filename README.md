# Exam Schedule 📅

Exam Schedule is a Spring Boot-based university scheduling platform designed to automate and optimize the process of organizing exam sessions. The system helps professors and administrators manage exam planning faster, more accurately, and with fewer scheduling conflicts.

## Features

* Automated exam scheduling using AI
* Professor exam session requests
* Student count estimation per exam
* Classroom capacity management
* Conflict detection and schedule optimization
* Drag-and-drop calendar for manual adjustments
* Admin control over final scheduling decisions
* Integration with FINKI university database
* Real-time schedule updates and management

## How It Works

Professors submit information about their exams, including the expected number of students for each exam session. This data is essential for determining classroom requirements, available time slots, and avoiding conflicts between exams.

The platform uses a Gemini AI model to analyze:

* Classroom availability
* Professor availability
* Student group conflicts
* Room capacity
* Scheduling efficiency
* Time slot optimization

Based on this analysis, the system generates an optimized exam schedule with approximately 85%–90% accuracy.

## Admin Features

Administrators can:

* Review the AI-generated schedule
* Manually adjust exam sessions
* Use drag-and-drop calendar editing
* Resolve special scheduling conflicts
* Approve the final exam timetable
* Manage classrooms and exam sessions

This ensures full human control over the final result while benefiting from AI automation.

## Technologies Used

### Backend

* Spring Boot
* Java
* REST APIs

### Database

* FINKI University Database
* PostgreSQL / MySQL (depending on implementation)

### AI Integration

* Gemini AI Model

### Additional Tools

* Docker
* Maven
* GitHub

## Project Goal

The goal of Exam Schedule is to reduce the complexity of manual exam scheduling in universities, especially for large institutions with hundreds of exams and limited classroom resources.

By combining artificial intelligence with administrator supervision, the platform creates a smarter, faster, and more reliable scheduling process.

## Future Improvements

* Student-side schedule notifications
* Email reminders for professors and students
* Advanced conflict prediction
* Calendar export (Google Calendar / Outlook)
* Multi-university support
* Reporting and analytics dashboard

---

Exam Schedule combines Spring Boot, AI-powered optimization, and real university data to deliver a modern solution for academic exam management.
