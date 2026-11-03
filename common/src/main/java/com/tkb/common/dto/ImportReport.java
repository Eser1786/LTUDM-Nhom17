package com.tkb.common.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * B2.4 - ImportReport: Bao cao ket qua phan tich va nhap file PDF thoi khoa bieu
 * (tra ve trong JOB_DONE cua lenh IMPORT_COMMIT - Muc 8 Protocol).
 *
 * Cau truc JSON chuan:
 *   {
 *     "dryRun": true,
 *     "detectedRows": 95,
 *     "courses": {"inserted": 5, "updated": 0},
 *     "sections": {"inserted": 58, "updated": 0, "deactivated": 0},
 *     "errors": [{"row": 53, "message": "..."}],
 *     "warnings": [{"row": 81, "message": "..."}]
 *   }
 *
 * Vi tri dat file:
 *   common/src/main/java/com/tkb/common/dto/ImportReport.java
 */
public class ImportReport {

    private boolean dryRun;
    private int detectedRows;
    private CourseStats courses = new CourseStats();
    private SectionStats sections = new SectionStats();
    private List<RowIssue> errors = new ArrayList<>();
    private List<RowIssue> warnings = new ArrayList<>();

    public ImportReport() {
    }

    public ImportReport(
            boolean dryRun,
            int detectedRows,
            CourseStats courses,
            SectionStats sections,
            List<RowIssue> errors,
            List<RowIssue> warnings) {
        this.dryRun = dryRun;
        this.detectedRows = detectedRows;
        this.courses = (courses != null) ? courses : new CourseStats();
        this.sections = (sections != null) ? sections : new SectionStats();
        this.errors = (errors != null) ? errors : new ArrayList<>();
        this.warnings = (warnings != null) ? warnings : new ArrayList<>();
    }

    public void addError(int sttRow, String message) {
        this.errors.add(new RowIssue(sttRow, message));
    }

    public void addWarning(int sttRow, String message) {
        this.warnings.add(new RowIssue(sttRow, message));
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public int getDetectedRows() {
        return detectedRows;
    }

    public void setDetectedRows(int detectedRows) {
        this.detectedRows = detectedRows;
    }

    public CourseStats getCourses() {
        return courses;
    }

    public void setCourses(CourseStats courses) {
        this.courses = courses;
    }

    public SectionStats getSections() {
        return sections;
    }

    public void setSections(SectionStats sections) {
        this.sections = sections;
    }

    public List<RowIssue> getErrors() {
        return errors;
    }

    public void setErrors(List<RowIssue> errors) {
        this.errors = errors;
    }

    public List<RowIssue> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<RowIssue> warnings) {
        this.warnings = warnings;
    }

    public static class CourseStats {
        private int inserted;
        private int updated;

        public CourseStats() {
        }

        public CourseStats(int inserted, int updated) {
            this.inserted = inserted;
            this.updated = updated;
        }

        public int getInserted() {
            return inserted;
        }

        public void setInserted(int inserted) {
            this.inserted = inserted;
        }

        public int getUpdated() {
            return updated;
        }

        public void setUpdated(int updated) {
            this.updated = updated;
        }
    }

    public static class SectionStats {
        private int inserted;
        private int updated;
        private int deactivated;

        public SectionStats() {
        }

        public SectionStats(int inserted, int updated, int deactivated) {
            this.inserted = inserted;
            this.updated = updated;
            this.deactivated = deactivated;
        }

        public int getInserted() {
            return inserted;
        }

        public void setInserted(int inserted) {
            this.inserted = inserted;
        }

        public int getUpdated() {
            return updated;
        }

        public void setUpdated(int updated) {
            this.updated = updated;
        }

        public int getDeactivated() {
            return deactivated;
        }

        public void setDeactivated(int deactivated) {
            this.deactivated = deactivated;
        }
    }

    public static class RowIssue {
        private int row;
        private String message;

        public RowIssue() {
        }

        public RowIssue(int row, String message) {
            this.row = row;
            this.message = message;
        }

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}