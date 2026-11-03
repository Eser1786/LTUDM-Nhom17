package com.tkb.common.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * B2.4 - SectionDto: Cau truc du lieu mot nhom lop hoc phan (Muc 8 Protocol).
 * Vẫn giữ đầy đủ trường phân biệt Lý thuyết / Thực hành (type = "LT" hoặc "TH")
 * và tổ thực hành (labGroup = "" nếu là LT, hoặc "01", "02"... nếu là TH).
 *
 * Cau truc JSON chuan:
 *   {
 *     "id": 40,
 *     "courseCode": "841109",
 *     "courseName": "Cơ sở dữ liệu",
 *     "credits": 4,
 *     "groupNo": "01",
 *     "labGroup": "",
 *     "type": "LT",
 *     "lecturer": "Trương Tấn Khoa",
 *     "lecturerCode": "11384",
 *     "className": "DCT1251",
 *     "capacity": 50,
 *     "active": true,
 *     "slots": [SlotDto]
 *   }
 *
 * Vi tri dat file:
 *   common/src/main/java/com/tkb/common/dto/SectionDto.java
 */
public class SectionDto {

    public static final String TYPE_LT = "LT";
    public static final String TYPE_TH = "TH";

    private Long id;
    private String semesterCode;
    private String courseCode;
    private String courseName;
    private Integer credits;
    private String groupNo;
    private String labGroup;
    private String type; // "LT" hoac "TH"
    private String lecturer;
    private String lecturerCode;
    private String className;
    private int capacity;
    private boolean active = true;
    private List<SlotDto> slots = new ArrayList<>();

    public SectionDto() {
    }

    public SectionDto(
            Long id,
            String courseCode,
            String courseName,
            Integer credits,
            String groupNo,
            String labGroup,
            String type,
            String lecturer,
            String lecturerCode,
            String className,
            int capacity,
            boolean active,
            List<SlotDto> slots) {
        this.id = id;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.credits = credits;
        this.groupNo = groupNo;
        this.labGroup = (labGroup != null) ? labGroup : "";
        this.type = (type != null && !type.isBlank()) ? type : TYPE_LT;
        this.lecturer = lecturer;
        this.lecturerCode = lecturerCode;
        this.className = className;
        this.capacity = capacity;
        this.active = active;
        this.slots = (slots != null) ? slots : new ArrayList<>();
    }

    /**
     * Kiem tra nhom lop nay co phai la To Thuc Hanh (TH) hay khong.
     */
    public boolean isPractice() {
        return TYPE_TH.equalsIgnoreCase(type);
    }

    /**
     * Kiem tra nhom lop nay co phai la Ly Thuyet (LT) hay khong.
     */
    public boolean isTheory() {
        return TYPE_LT.equalsIgnoreCase(type);
    }

    /**
     * Kiem tra nhom lop nay co trung lich voi mot nhom lop khac khong (xet ca Tuan hoc).
     */
    public boolean conflictsWith(SectionDto other) {
        if (other == null || this.slots == null || other.slots == null) {
            return false;
        }
        for (SlotDto s1 : this.slots) {
            for (SlotDto s2 : other.slots) {
                if (s1.conflictsWith(s2)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSemesterCode() {
        return semesterCode;
    }

    public void setSemesterCode(String semesterCode) {
        this.semesterCode = semesterCode;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Integer getCredits() {
        return credits;
    }

    public void setCredits(Integer credits) {
        this.credits = credits;
    }

    public String getGroupNo() {
        return groupNo;
    }

    public void setGroupNo(String groupNo) {
        this.groupNo = groupNo;
    }

    public String getLabGroup() {
        return labGroup != null ? labGroup : "";
    }

    public void setLabGroup(String labGroup) {
        this.labGroup = (labGroup != null) ? labGroup : "";
    }

    public String getType() {
        return type != null ? type : TYPE_LT;
    }

    public void setType(String type) {
        this.type = (type != null && !type.isBlank()) ? type : TYPE_LT;
    }

    public String getLecturer() {
        return lecturer;
    }

    public void setLecturer(String lecturer) {
        this.lecturer = lecturer;
    }

    public String getLecturerCode() {
        return lecturerCode;
    }

    public void setLecturerCode(String lecturerCode) {
        this.lecturerCode = lecturerCode;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<SlotDto> getSlots() {
        return slots;
    }

    public void setSlots(List<SlotDto> slots) {
        this.slots = (slots != null) ? slots : new ArrayList<>();
    }
}