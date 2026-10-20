package com.tkb.frontend.net;

import com.tkb.frontend.model.Types;
import com.tkb.frontend.model.Types.Course;
import com.tkb.frontend.model.Types.JobStatus;
import com.tkb.frontend.model.Types.PlanDetail;
import com.tkb.frontend.model.Types.PlanRequest;
import com.tkb.frontend.model.Types.SavedPlan;
import com.tkb.frontend.model.Types.Semester;
import com.tkb.frontend.model.Types.Student;

import java.util.List;
/**
 * Giao thuc goi server. Ban that dung socket/HTTP; ban dev dung MockNetworkClient.
 * Moi lenh deu nem ApiException khi that bai de lop UI doi thanh thong bao than thien.
 */
public interface TkbClient {

    /** STUDENT_INFO - thong tin sinh vien hien tai (dang nhap). */
    Student sinhVien() throws ApiException;

    /** LIST_SEMESTERS */
    List<Semester> listSemesters() throws ApiException;

    /** LIST_COURSES - hoc phan co nhom lop dang hoat dong trong hoc ky. */
    List<Course> listCourses(String maHocKy) throws ApiException;

    /** Cac nhom lop cua mot hoc phan trong hoc ky, dung cho trang nhom lop hoc phan. */
    List<Types.SectionSlot> listSections(String maHocKy, String maMh) throws ApiException;

    /** Tat ca nhom lop dang hoat dong trong hoc ky (nhom lop theo hoc phan). */
    List<Types.SectionSlot> listAllSections(String maHocKy) throws ApiException;

    /** SUBMIT_REQUEST - tra ve jobId. Nhung hoc phan khong mo se nem COURSE_NOT_OPEN. */
    String submit(PlanRequest request) throws ApiException;

    /** JOB_PROGRESS */
    JobStatus jobStatus(String jobId) throws ApiException;

    /** CANCEL_JOB */
    void cancel(String jobId) throws ApiException;

    /** LIST_PLANS - loc theo hoc ky, null nghia la tat ca. */
    List<SavedPlan> listPlans(String maHocKy) throws ApiException;

    /** GET_PLAN */
    PlanDetail getPlan(long planId) throws ApiException;

    /** SAVE_PLAN - chi gui sectionIds; nem NAME_TAKEN neu trung ten. */
    long savePlan(String maHocKy, String ten, List<Integer> sectionIds) throws ApiException;

    /** DELETE_PLAN */
    void deletePlan(long planId) throws ApiException;

    /** RENAME_PLAN - nem NAME_TAKEN neu ten da duoc dung. */
    void renamePlan(long planId, String tenMoi) throws ApiException;
}
