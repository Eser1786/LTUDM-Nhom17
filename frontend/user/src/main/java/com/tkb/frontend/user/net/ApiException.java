package com.tkb.frontend.user.net;

import com.tkb.frontend.user.model.ErrorCode;

import java.util.List;

/** Loi nghiep vu tra ve tu server, mang theo ErrorCode va du lieu kem theo. */
public class ApiException extends Exception {
    private final ErrorCode code;
    private final List<String> details;

    public ApiException(ErrorCode code, String message) {
        this(code, message, List.of());
    }

    public ApiException(ErrorCode code, String message, List<String> details) {
        super(message);
        this.code = code;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public ErrorCode code() {
        return code;
    }

    /** Du lieu bo sung, vi du danh sach ma hoc phan khong mo. */
    public List<String> details() {
        return details;
    }
}
