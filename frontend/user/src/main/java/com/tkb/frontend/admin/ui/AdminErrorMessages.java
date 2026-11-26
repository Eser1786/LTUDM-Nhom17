package com.tkb.frontend.admin.ui;

import com.tkb.common.protocol.ErrorCode;
import com.tkb.common.protocol.Response;

import java.util.Locale;

final class AdminErrorMessages {
    private static final String UNKNOWN_ERROR =
            "Máy chủ gặp lỗi khi xử lý yêu cầu. Vui lòng thử lại sau.";

    private AdminErrorMessages() {
    }

    static String from(Response.ErrorPayload error) {
        return from(
                error == null ? null : error.getCode(),
                error == null ? null : error.getMessage());
    }

    static String from(String rawCode, String backendMessage) {
        if (rawCode == null || rawCode.isBlank()) {
            return UNKNOWN_ERROR;
        }

        ErrorCode code;
        try {
            code = ErrorCode.valueOf(rawCode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return UNKNOWN_ERROR;
        }

        return switch (code) {
            case BAD_REQUEST ->
                    "Yêu cầu không hợp lệ. Vui lòng kiểm tra lại dữ liệu.";
            case UNAUTHENTICATED ->
                    "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
            case FORBIDDEN ->
                    "Tài khoản không có quyền thực hiện thao tác này.";
            case INVALID_CREDENTIALS ->
                    "Tên đăng nhập hoặc mật khẩu không đúng.";
            case USERNAME_TAKEN ->
                    "Tên đăng nhập này đã được sử dụng.";
            case NAME_TAKEN ->
                    "Tên phương án đã tồn tại. Hãy chọn tên khác.";
            case VALIDATION_FAILED ->
                    backendMessage == null || backendMessage.isBlank()
                            ? ErrorCode.VALIDATION_FAILED.getDefaultMessage()
                            : backendMessage.trim();
            case NOT_FOUND ->
                    "Dữ liệu không còn tồn tại. Hãy tải lại danh sách rồi thử lại.";
            case SEMESTER_NOT_FOUND ->
                    "Không tìm thấy học kỳ. Hãy tải lại và chọn học kỳ khác.";
            case SEMESTER_EXISTS ->
                    "Mã học kỳ này đã tồn tại. Hãy nhập mã khác.";
            case COURSE_NOT_OPEN ->
                    "Một hoặc nhiều học phần không được mở trong học kỳ đã chọn.";
            case COURSE_IN_USE ->
                    "Không thể xóa học phần vì đang có nhóm lớp hoặc phương án đã lưu sử dụng.";
            case SECTION_EXISTS ->
                    "Nhóm lớp này đã tồn tại trong học kỳ. Hãy kiểm tra lại số nhóm.";
            case SECTION_IN_USE ->
                    "Không thể xóa nhóm lớp vì đang được phương án đã lưu sử dụng. Hãy tắt nhóm thay vì xóa.";
            case JOB_NOT_FOUND ->
                    "Không tìm thấy công việc. Công việc có thể đã hoàn tất hoặc hết hạn.";
            case TOO_MANY_JOBS ->
                    "Đang có quá nhiều công việc chạy. Vui lòng chờ rồi thử lại.";
            case UPLOAD_NOT_FOUND ->
                    "Phiên tải lên đã hết hạn hoặc không còn tồn tại. Vui lòng tải tệp lại.";
            case CHUNK_OUT_OF_ORDER ->
                    "Các phần của tệp được gửi sai thứ tự. Vui lòng tải tệp lên lại.";
            case MISSING_CHUNKS ->
                    "Tệp tải lên chưa đầy đủ. Vui lòng tải tệp lên lại.";
            case CANNOT_MODIFY_SELF ->
                    "Quản trị viên không thể tự đổi vai trò, tự xóa tài khoản hoặc tự ngắt phiên của mình.";
            case INTERNAL_ERROR -> UNKNOWN_ERROR;
                case OTP_EXPIRED -> throw new UnsupportedOperationException("Unimplemented case: " + code);
                case OTP_INVALID -> throw new UnsupportedOperationException("Unimplemented case: " + code);
                case OTP_TOO_MANY_ATTEMPTS -> throw new UnsupportedOperationException("Unimplemented case: " + code);
                default -> throw new IllegalArgumentException("Unexpected value: " + code);
        };
    }
}
