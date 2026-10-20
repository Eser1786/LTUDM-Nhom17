package com.tkb.frontend.ui;

import com.tkb.frontend.model.ErrorCode;
import com.tkb.frontend.net.ApiException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * D6. Doi ErrorCode thanh thong bao than thien cho nguoi dung.
 * Khong bao gio hien ma loi tran cho nguoi dung cuoi.
 */
public final class ErrorMessages {

    private ErrorMessages() {
    }

    public static String thanThien(ErrorCode code) {
        if (code == null) {
            return "Đã xảy ra lỗi không xác định. Vui lòng thử lại.";
        }
        return switch (code) {
            case COURSE_NOT_OPEN -> "Một số học phần bạn chọn không được mở trong học kỳ này.";
            case NAME_TAKEN -> "Tên phương án này đã tồn tại. Hãy đặt tên khác.";
            case JOB_CANCELLED -> "Bạn đã hủy quá trình xếp lịch.";
            case JOB_FAILED -> "Quá trình xếp lịch thất bại. Vui lòng thử lại.";
            case JOB_NOT_FOUND -> "Không tìm thấy tiến trình xếp lịch. Vui lòng tạo lại yêu cầu.";
            case PLAN_NOT_FOUND -> "Không tìm thấy phương án đã lưu. Có thể nó đã bị xóa.";
            case SEMESTER_NOT_FOUND -> "Học kỳ không tồn tại. Vui lòng chọn lại.";
            case VALIDATION_ERROR -> "Dữ liệu nhập chưa hợp lệ.";
            case NETWORK_ERROR -> "Không kết nối được máy chủ. Kiểm tra lại kết nối rồi thử lại.";
            case EXPORT_ERROR -> "Không ghi được tệp ảnh. Có thể tệp đang mở hoặc ổ đĩa đầy.";
            case UNKNOWN -> "Đã xảy ra lỗi không xác định. Vui lòng thử lại.";
        };
    }

    /** Thong bao day du cho mot ApiException, kem chi tiet neu co. */
    public static String thanThien(ApiException ex) {
        StringBuilder sb = new StringBuilder(thanThien(ex.code()));
        List<String> details = ex.details();
        if (!details.isEmpty()) {
            sb.append("\n");
            if (ex.code() == ErrorCode.COURSE_NOT_OPEN) {
                sb.append("Học phần không mở: ");
            } else {
                sb.append("Chi tiết: ");
            }
            sb.append(details.stream().collect(Collectors.joining(", ")));
        } else if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            sb.append("\n").append(ex.getMessage());
        }
        return sb.toString();
    }
}
