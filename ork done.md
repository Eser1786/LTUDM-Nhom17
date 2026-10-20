work done
Created frontend user UI (Swing, Java 21) under frontend/src/main/java/com/tkb/frontend/:
- model/ErrorCode.java - error codes
- model/Types.java - DTOs (Semester, Course, Section, ClassSession, SectionSlot, Criteria, PlanRequest, OptionStats, ScheduleOption, JobResult, JobStatus, SavedPlan, PlanDetail)
- net/ApiException.java - business exception with code/details
- net/TkbClient.java - client API (listSemesters, listCourses, listSections, submit, jobStatus, cancel, listPlans, getPlan, savePlan, deletePlan)
- net/MockNetworkClient.java - fake data + job simulation + greedy scheduler producing ranked options
- ui/ErrorMessages.java - friendly error mapping
- ui/Functions.java - dialog helpers
- ui/TimetableGridPanel.java - reusable week grid (Mon-Sun × 12 periods), per-course colors, LT/TH styles, PNG export
- ui/RequirementPanel.java - semester select (defaults to current), course search/list, priority reorder (up/down/remove), criteria (checkboxes + weights), locked section (D7), file import
- ui/JobPanel.java - progress bar, cancel, polling jobStatus
- ui/ResultPanel.java - options list with stats, timetable view, excluded courses notice, alternatives, save (NAME_TAKEN handling), PNG export
- ui/SavedPlansPanel.java - list/filter by semester, view, delete, side-by-side compare (two plans)
- ui/MainFrame.java - card layout navigation, menu
- Main.java - entry point
Built: mvn compile/package succeeds for common/backend/frontend.
Changes to backend? No source edits in backend/src/. Only build artifacts changed (backend/target/classes/... due to recompilation). All new files are in frontend/src/.
All requested user frontend tasks (D1–D7 core parts) are implemented with mock data as requested.