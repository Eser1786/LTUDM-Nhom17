package com.tkb.common.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * B2.3 - Response: Doi tuong dai dien cho phan hoi tu Server gui ve Client,
 * bao gom ca 3 loai thong diep theo Muc 4 trang Protocol:
 *
 * 1. Response thanh cong:
 *    {"id": 12, "ok": true, "data": {...}}
 *
 * 2. Response loi:
 *    {"id": 12, "ok": false, "error": {"code": "INVALID_CREDENTIALS", "message": "...", "details": {}}}
 *
 * 3. Goi Push (Server chu dong day ve Client, khong co id, co truong push):
 *    {"push": "JOB_PROGRESS", "data": {...}}
 *    {"push": "JOB_DONE", "data": {...}}
 *
 * Vi tri dat file:
 *   common/src/main/java/com/tkb/common/protocol/Response.java
 */
public class Response {

    /** ID tuong ung voi Request (null neu la goi Push) */
    private Integer id;

    /** Trang thai thanh cong/that bai (null neu la goi Push) */
    private Boolean ok;

    /** Ten su kien Push: "JOB_PROGRESS" hoac "JOB_DONE" (null neu la Response thuong) */
    private String push;

    /** Du lieu tra ve khi ok = true hoac khi la goi Push */
    private Map<String, Object> data;

    /** Thong tin loi chi tiet khi ok = false */
    private ErrorPayload error;

    public Response() {
    }

    // =========================================================================
    // CAC HAM KHOI TAO NHANH (FACTORY METHODS) CHO SERVER
    // =========================================================================

    /**
     * Tao Response thanh cong voi Map data.
     */
    public static Response success(Integer requestId, Map<String, Object> data) {
        Response res = new Response();
        res.id = requestId;
        res.ok = true;
        res.data = (data != null) ? data : new LinkedHashMap<>();
        return res;
    }

    /**
     * Tao Response thanh cong rong (data = {}).
     */
    public static Response successEmpty(Integer requestId) {
        return success(requestId, new LinkedHashMap<>());
    }

    /**
     * Tao Response thanh cong chi co 1 cap key-value (vd: {"jobId": "a1b2c3"}).
     */
    public static Response successSingle(Integer requestId, String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(key, value);
        return success(requestId, map);
    }

    /**
     * Tao Response loi voi ma ErrorCode chuan va cau thong bao mac dinh tieng Viet.
     */
    public static Response error(Integer requestId, ErrorCode code) {
        return error(requestId, code, code.getDefaultMessage(), null);
    }

    /**
     * Tao Response loi voi thong bao tuy chinh.
     */
    public static Response error(Integer requestId, ErrorCode code, String customMessage) {
        return error(requestId, code, customMessage, null);
    }

    /**
     * Tao Response loi kem chi tiet details (vd: details.field hoac details.courseCodes).
     */
    public static Response error(Integer requestId, ErrorCode code, String customMessage, Map<String, Object> details) {
        Response res = new Response();
        res.id = requestId;
        res.ok = false;
        res.error = new ErrorPayload(
                code.name(),
                (customMessage != null && !customMessage.isBlank()) ? customMessage : code.getDefaultMessage(),
                details != null ? details : new LinkedHashMap<>()
        );
        return res;
    }

    /**
     * Tao goi Push do Server chu dong day ve Client (JOB_PROGRESS hoac JOB_DONE).
     * Goi Push khong co id va ok, chi co truong "push" va "data".
     */
    public static Response pushMessage(Command pushCommand, Map<String, Object> data) {
        Response res = new Response();
        res.push = pushCommand.name();
        res.data = (data != null) ? data : new LinkedHashMap<>();
        return res;
    }

    /**
     * Kiem tra xem goi tin nay co phai la goi Push tu Server hay khong.
     */
    public boolean isPush() {
        return push != null && !push.isBlank();
    }

    // =========================================================================
    // GETTERS & SETTERS
    // =========================================================================

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Boolean getOk() {
        return ok;
    }

    public void setOk(Boolean ok) {
        this.ok = ok;
    }

    public String getPush() {
        return push;
    }

    public void setPush(String push) {
        this.push = push;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public ErrorPayload getError() {
        return error;
    }

    public void setError(ErrorPayload error) {
        this.error = error;
    }

    /**
     * Cau truc doi tuong "error" ben trong Response khi ok = false:
     *   {"code": "VALIDATION_FAILED", "message": "...", "details": {"field": "username"}}
     */
    public static class ErrorPayload {
        private String code;
        private String message;
        private Map<String, Object> details;

        public ErrorPayload() {
            this.details = new LinkedHashMap<>();
        }

        public ErrorPayload(String code, String message, Map<String, Object> details) {
            this.code = code;
            this.message = message;
            this.details = (details != null) ? details : new LinkedHashMap<>();
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Map<String, Object> getDetails() {
            return details;
        }

        public void setDetails(Map<String, Object> details) {
            this.details = details;
        }
    }
}