# Frontend Function Structure

## 1) Entry point

### `Main` (`frontend/user/src/main/java/com/tkb/frontend/user/Main.java`)
- Purpose: JavaFX application entry point.
- Main functions:
  - `start(Stage stage)`: launches the JavaFX window and then creates the main app frame.
  - `main(String[] args)`: Java entry point that calls `Application.launch(...)`.

## 2) Main app shell

### `MainFrame` (`frontend/user/src/main/java/com/tkb/frontend/user/ui/MainFrame.java`)
- Purpose: main window layout, navigation, and overall app state.
- Main functions:
  - `MainFrame()`: initializes sidebar, header, cards, and login screen.
  - `taoNavItem(String card, String text)`: creates a navigation button.
  - `denTrang(String card)`: switches the visible screen card.
  - `sauDangNhap()`: loads student + semester data after login and enters the overview screen.
  - `capNhatHeader()`: updates user identity info in the app header.
  - `capNhatSidebarHocKy()`: updates selected semester information in the sidebar.
  - `batDauXepLich()`: starts the scheduling job from the current requirements.
  - `jobXong(JobResult result)`: handles a completed scheduler job and opens result screen.
  - `jobHuy()`: cancels the current job and returns to the priority screen.
  - `jobThatBai(String message)`: handles failed scheduling state.
  - `moSoSanhPhuongAn(String quayLaiCard, List<ScheduleOption> ds)`: opens comparison screen for proposed options.
  - `moSoSanhDaLuu(String quayLaiCard, List<Long> ids)`: opens comparison for saved plans.
  - `trangSoSanhQuayLai()`: returns the previous page for the compare screen.
  - `trangThai()`: returns UI state object.
  - `main(String[] args)`: placeholder for JavaFX startup (no-op).

### `AppUiState` (`MainFrame.java`)
- Purpose: stores application runtime state.
- Main functions:
  - `AppUiState(TkbClient client)`: constructor.
  - `chonMacDinh()`: selects default course-group values for demo usage.
  - `monDaChon()`: returns current selected course codes.

### `AvatarPanel` (`MainFrame.java`)
- Purpose: circular avatar showing student initials.
- Main functions:
  - `AvatarPanel(String chu, int kichThuoc)`: constructor.
  - `setChu(String chu)`: updates the avatar text.
  - `render()`: redraws the circle and label.

### `LoginPanel` (`MainFrame.java`)
- Purpose: login screen for demo access.
- Main functions:
  - `LoginPanel(AppUiState st, Runnable onLogin)`: constructor.

### `OverviewPanel` (`MainFrame.java`)
- Purpose: overview screen placeholder.
- Main functions:
  - `OverviewPanel(MainFrame frame)`: constructor.
  - `taiLai()`: refresh overview data.

### `PriorityPanel` (`MainFrame.java`)
- Purpose: priority-setting screen placeholder.
- Main functions:
  - `PriorityPanel(MainFrame frame)`: constructor.
  - `taiLai()`: refresh priority config.

### `ComparePanel` (`MainFrame.java`)
- Purpose: comparison screen placeholder for result comparison.
- Main functions:
  - `ComparePanel(MainFrame frame)`: constructor.
  - `taiDuLieu()`: loads compare data.
  - `napPhuongAn(List<ScheduleOption> ds)`: loads proposed plan comparison data.
  - `napDaLuu(List<Long> ids)`: loads saved-plan comparison data.

## 3) Shared UI helpers

### `Functions` (`frontend/user/src/main/java/com/tkb/frontend/user/ui/Functions.java`)
- Purpose: common JavaFX styling and reusable widgets.
- Main functions:
  - `the()`: returns a default white card container.
  - `the(int padding)`: returns a styled card with custom padding.
  - `chuDe(String text)`: creates a section title label.
  - `moTa(String text)`: creates a muted subtitle label.
  - `tieuDeThe(String text)`: creates a subsection header label.
  - `nutChinh(String text)`: creates the main action button.
  - `nutPhu(String text)`: creates a secondary button.
  - `nutXoa(String text)`: creates a delete-style button.
  - `nut(String text, Color bg, Color border, Color fg)`: internal shared button creator.
  - `nhan(String text, Color bg, Color fg)`: creates a badge.
  - `nhanHopLe`, `nhanDeXuat`, `nhanCanhBao`, `nhanXam`, `nhanLoi`, `nhanChinh`: badge variants.
  - `oNhap()`: creates a standard input text field.
  - `thongTin(Window parent, String message)`: info alert.
  - `canhBao(Window parent, String message)`: warning alert.
  - `loi(Window parent, ApiException ex)`: error alert from API exception.
  - `loi(Window parent, String message)`: error alert from a string message.
  - `code(ApiException ex)`: extract error code.
  - `showError(Window parent, String message)`: internal error dialog builder.
  - `dauTrang(String tieuDe, String moTa)`: builds a title/subtitle header row.
  - `toWeb(Paint paint)`: converts JavaFX color to CSS web color.

## 4) Requirement screen

### `RequirementPanel` (`frontend/user/src/main/java/com/tkb/frontend/user/ui/RequirementPanel.java`)
- Purpose: shows course sections and filtering UI.
- Main functions:
  - `RequirementPanel(TkbClient client, MainFrame frame)`: constructor, creates filter controls and screen layout.
  - `taiDuLieu()`: loads current section data.
  - `veBang()`: renders the section list (currently scaffolded in JavaFX).
  - `datLaiBoLoc()`: resets filters to default values.

## 5) Scheduling job screen

### `JobPanel` (`frontend/user/src/main/java/com/tkb/frontend/user/ui/JobPanel.java`)
- Purpose: shows current optimization job progress.
- Main functions:
  - `JobPanel(TkbClient client, MainFrame frame)`: constructor and layout creation.
  - `batDau(String jobId)`: starts the progress display for a new job.
  - `dung()`: stops or freezes the running job display.
  - `thuLai()`: retry / reset action for the job panel.
  - `hienLoi(String message)`: displays the error message from job processing.
  - `capNhat(JobStatus jobStatus)`: updates progress and status text based on server result.

## 6) Result screen

### `ResultPanel` (`frontend/user/src/main/java/com/tkb/frontend/user/ui/ResultPanel.java`)
- Purpose: displays recommended schedule options.
- Main functions:
  - `ResultPanel(TkbClient client, MainFrame frame)`: constructor.
  - `hienTrangThai()`: shows either a placeholder or current result list.
  - `hienKetQua(JobResult result)`: stores and renders a result set.
  - `xuatAnh()`: export result to PNG (currently stubbed).
  - `moSoSanh()`: compare selected schedule options (currently stubbed).
  - `luuPhuongAn()`: save a chosen plan (currently stubbed).
  - `veLai()`: revert to previous view (currently stubbed).

## 7) Saved plans screen

### `SavedPlansPanel` (`frontend/user/src/main/java/com/tkb/frontend/user/ui/SavedPlansPanel.java`)
- Purpose: lists saved timetable plans and allows search / compare.
- Main functions:
  - `SavedPlansPanel(TkbClient client, MainFrame frame)`: constructor and layout.
  - `taiDanhSach()`: loads saved plans from the backend client.
  - `loc()`: filters plans by text search.
  - `veDanhSach()`: draws the cards list.
  - `capNhatNutSoSanh()`: enables/disables compare action based on selected items.
  - `moLich(SavedPlan p)`: opens a saved plan view.
  - `doiTen(SavedPlan p)`: rename a saved plan (stubbed).
  - `xoa(SavedPlan p)`: delete a saved plan (stubbed).
  - `moSoSanh()`: compare selected saved plans (stubbed).

## 8) Timetable grid view

### `TimetableGridPanel` (`frontend/user/src/main/java/com/tkb/frontend/user/ui/TimetableGridPanel.java`)
- Purpose: draws a weekly timetable grid with course cells.
- Main functions:
  - `TimetableGridPanel()`: constructor and base grid initialization.
  - `setTieuDe(String tieuDe)`: sets the schedule title.
  - `setSlots(List<SectionSlot> slots)`: loads timetable data and redraws the grid.
  - `veMacDinh()`: creates empty weekday/time grid layout.
  - `veLich()`: populates cells using section slots.
  - `colorFor(String code)`: assigns consistent colors to courses.
  - `indexOfDay(String day)`: converts day text to a grid column index.
  - `indexOfHour(String hour)`: converts time text to a grid row index.
  - `dayLabel(int index)`: returns weekday label.
  - `timeLabel(int index)`: returns time label.

## 9) Net/client layer used by frontend

### `TkbClient` / `MockNetworkClient`
- Purpose: backend abstraction used by the frontend for mocked timetable operations.
- Main operations used by UI:
  - `sinhVien()`
  - `listSemesters()`
  - `submit(PlanRequest request)`
  - `jobStatus(String jobId)`
  - `listPlans(String maHocKy)`
  - `getPlan(long planId)`
  - `savePlan(...)`
  - `deletePlan(...)`
  - `renamePlan(...)`

## 10) Data model used by UI

### `Types` (`frontend/user/src/main/java/com/tkb/frontend/user/model/Types.java`)
- Purpose: domain data records for semester, sections, criteria, jobs, results, and saved plans.
- Important records:
  - `Semester`
  - `Student`
  - `Course`
  - `Section`
  - `ClassSession`
  - `SectionSlot`
  - `Criteria`
  - `PlanRequest`
  - `OptionStats`
  - `ScheduleOption`
  - `JobResult`
  - `JobStatus`
  - `SavedPlan`
  - `PlanDetail`

## App flow summary
1. `Main` launches JavaFX app.
2. `MainFrame` shows login screen.
3. After login, UI loads semester and student info.
4. User selects sections in `RequirementPanel`.
5. User defines priorities in `PriorityPanel`.
6. Scheduler job starts in `JobPanel`.
7. `ResultPanel` shows recommended plans.
8. `SavedPlansPanel` and `TimetableGridPanel` handle saved and detailed view rendering.
