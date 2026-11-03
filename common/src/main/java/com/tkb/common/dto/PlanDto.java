package com.tkb.common.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * B2.4 - PlanDto: Cau truc du lieu mot phuong an thoi khoa bieu (Muc 8 Protocol).
 * Dung cho ket qua OPTIMIZE (trong JOB_DONE) va cac lenh LIST_PLANS, GET_PLAN.
 *
 * Cau truc JSON chuan:
 *   {
 *     "id": 17,
 *     "name": "Phương án ít ngày",
 *     "semesterCode": "2026-2027-1",
 *     "rank": 1,
 *     "score": 87.5,
 *     "metrics": {"studyDays": 4, "gapPeriods": 2, "morningPeriods": 18},
 *     "sections": [SectionDto],
 *     "droppedCourses": [{"courseCode": "841423", "name": "..."}],
 *     "createdAt": "2026-10-31T09:30:00"
 *   }
 *
 * Vi tri dat file:
 *   common/src/main/java/com/tkb/common/dto/PlanDto.java
 */
public class PlanDto {

    private Long id;
    private String name;
    private String semesterCode;
    private int rank;
    private double score;
    private MetricsDto metrics;
    private List<SectionDto> sections = new ArrayList<>();
    private List<DroppedCourseDto> droppedCourses = new ArrayList<>();
    private String createdAt;

    public PlanDto() {
        this.metrics = new MetricsDto();
    }

    public PlanDto(
            int rank,
            double score,
            MetricsDto metrics,
            List<SectionDto> sections,
            List<DroppedCourseDto> droppedCourses) {
        this.rank = rank;
        this.score = score;
        this.metrics = (metrics != null) ? metrics : new MetricsDto();
        this.sections = (sections != null) ? sections : new ArrayList<>();
        this.droppedCourses = (droppedCourses != null) ? droppedCourses : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSemesterCode() {
        return semesterCode;
    }

    public void setSemesterCode(String semesterCode) {
        this.semesterCode = semesterCode;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public MetricsDto getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricsDto metrics) {
        this.metrics = metrics;
    }

    public List<SectionDto> getSections() {
        return sections;
    }

    public void setSections(List<SectionDto> sections) {
        this.sections = (sections != null) ? sections : new ArrayList<>();
    }

    public List<DroppedCourseDto> getDroppedCourses() {
        return droppedCourses;
    }

    public void setDroppedCourses(List<DroppedCourseDto> droppedCourses) {
        this.droppedCourses = (droppedCourses != null) ? droppedCourses : new ArrayList<>();
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Cac chi so danh gia phuong an thoi khoa bieu:
     *   - studyDays: So ngay phai den truong trong tuan.
     *   - gapPeriods: Tong so tiet trong giua cac ca hoc trong cung mot ngay.
     *   - morningPeriods: Tong so tiet hoc buoi sang (tiet 1-5 theo quy uoc A4).
     */
    public static class MetricsDto {
        private int studyDays;
        private int gapPeriods;
        private int morningPeriods;

        public MetricsDto() {
        }

        public MetricsDto(int studyDays, int gapPeriods, int morningPeriods) {
            this.studyDays = studyDays;
            this.gapPeriods = gapPeriods;
            this.morningPeriods = morningPeriods;
        }

        public int getStudyDays() {
            return studyDays;
        }

        public void setStudyDays(int studyDays) {
            this.studyDays = studyDays;
        }

        public int getGapPeriods() {
            return gapPeriods;
        }

        public void setGapPeriods(int gapPeriods) {
            this.gapPeriods = gapPeriods;
        }

        public int getMorningPeriods() {
            return morningPeriods;
        }

        public void setMorningPeriods(int morningPeriods) {
            this.morningPeriods = morningPeriods;
        }
    }

    /**
     * Thong tin hoc phan bi loai khi khong the xep du tat ca cac mon (Yeu cau A.6).
     */
    public static class DroppedCourseDto {
        private String courseCode;
        private String name;

        public DroppedCourseDto() {
        }

        public DroppedCourseDto(String courseCode, String name) {
            this.courseCode = courseCode;
            this.name = name;
        }

        public String getCourseCode() {
            return courseCode;
        }

        public void setCourseCode(String courseCode) {
            this.courseCode = courseCode;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}