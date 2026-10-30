package com.tkb.common.protocol;

/**
 * B2.3 - Enum ErrorCode: Danh sach 21 ma loi chuan hoa theo trang Protocol (Muc 10).
 * Client cua Tran va Quy dua vao ma nay de hien thi thong bao hoac xu ly logic,
 * khong so sanh chuoi message.
 *
 * Vi tri dat file:
 *   common/src/main/java/com/tkb/common/ErrorCode.java
 */
public enum ErrorCode {

    BAD_REQUEST("Yêu cầu không hợp lệ hoặc sai cấu trúc gói tin."),
    UNAUTHENTICATED("Bạn cần đăng nhập để thực hiện chức năng này."),
    FORBIDDEN("Tài khoản của bạn không có quyền thực hiện thao tác này."),
    INVALID_CREDENTIALS("Tên đăng nhập hoặc mật khẩu không đúng."),
    USERNAME_TAKEN("Tên đăng nhập này đã được sử dụng."),
    NAME_TAKEN("Tên phương án thời khóa biểu đã tồn tại."),
    VALIDATION_FAILED("Dữ liệu đầu vào không hợp lệ."),
    NOT_FOUND("Không tìm thấy dữ liệu yêu cầu."),
    SEMESTER_NOT_FOUND("Không tìm thấy học kỳ yêu cầu."),
    SEMESTER_EXISTS("Mã học kỳ này đã tồn tại."),
    COURSE_NOT_OPEN("Một hoặc nhiều học phần không mở lớp trong học kỳ này."),
    COURSE_IN_USE("Không thể xóa học phần vì đang có nhóm lớp hoặc nằm trong phương án đã lưu."),
    SECTION_EXISTS("Nhóm lớp này đã tồn tại trong học kỳ."),
    SECTION_IN_USE("Không thể xóa nhóm lớp vì đang nằm trong phương án đã lưu (hãy dùng nút Tắt nhóm)."),
    JOB_NOT_FOUND("Không tìm thấy tiến trình xử lý yêu cầu."),
    TOO_MANY_JOBS("Bạn đang chạy tối đa 3 yêu cầu cùng lúc, vui lòng chờ hoàn tất."),
    UPLOAD_NOT_FOUND("Phiên tải file không tồn tại hoặc đã hết hạn."),
    CHUNK_OUT_OF_ORDER("Gói dữ liệu tải lên không đúng thứ tự."),
    MISSING_CHUNKS("File tải lên còn thiếu dữ liệu."),
    CANNOT_MODIFY_SELF("Quản trị viên không thể tự đổi vai trò, tự xóa hoặc tự ngắt phiên của chính mình."),
    INTERNAL_ERROR("Lỗi hệ thống phía máy chủ, vui lòng thử lại sau.");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}