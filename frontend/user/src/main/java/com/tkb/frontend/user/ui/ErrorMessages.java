package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.model.ErrorCode;
import com.tkb.frontend.user.net.ApiException;

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
            return "ÄÃ£ xáº£y ra lá»—i khÃ´ng xÃ¡c Ä‘á»‹nh. Vui lÃ²ng thá»­ láº¡i.";
        }
        return switch (code) {
            case COURSE_NOT_OPEN -> "Má»™t sá»‘ há»c pháº§n báº¡n chá»n khÃ´ng Ä‘Æ°á»£c má»Ÿ trong há»c ká»³ nÃ y.";
            case NAME_TAKEN -> "TÃªn phÆ°Æ¡ng Ã¡n nÃ y Ä‘Ã£ tá»“n táº¡i. HÃ£y Ä‘áº·t tÃªn khÃ¡c.";
            case JOB_CANCELLED -> "Báº¡n Ä‘Ã£ há»§y quÃ¡ trÃ¬nh xáº¿p lá»‹ch.";
            case JOB_FAILED -> "QuÃ¡ trÃ¬nh xáº¿p lá»‹ch tháº¥t báº¡i. Vui lÃ²ng thá»­ láº¡i.";
            case JOB_NOT_FOUND -> "KhÃ´ng tÃ¬m tháº¥y tiáº¿n trÃ¬nh xáº¿p lá»‹ch. Vui lÃ²ng táº¡o láº¡i yÃªu cáº§u.";
            case PLAN_NOT_FOUND -> "KhÃ´ng tÃ¬m tháº¥y phÆ°Æ¡ng Ã¡n Ä‘Ã£ lÆ°u. CÃ³ thá»ƒ nÃ³ Ä‘Ã£ bá»‹ xÃ³a.";
            case SEMESTER_NOT_FOUND -> "Há»c ká»³ khÃ´ng tá»“n táº¡i. Vui lÃ²ng chá»n láº¡i.";
            case VALIDATION_ERROR -> "Dá»¯ liá»‡u nháº­p chÆ°a há»£p lá»‡.";
            case NETWORK_ERROR -> "KhÃ´ng káº¿t ná»‘i Ä‘Æ°á»£c mÃ¡y chá»§. Kiá»ƒm tra láº¡i káº¿t ná»‘i rá»“i thá»­ láº¡i.";
            case EXPORT_ERROR -> "KhÃ´ng ghi Ä‘Æ°á»£c tá»‡p áº£nh. CÃ³ thá»ƒ tá»‡p Ä‘ang má»Ÿ hoáº·c á»• Ä‘Ä©a Ä‘áº§y.";
            case UNKNOWN -> "ÄÃ£ xáº£y ra lá»—i khÃ´ng xÃ¡c Ä‘á»‹nh. Vui lÃ²ng thá»­ láº¡i.";
        };
    }

    /** Thong bao day du cho mot ApiException, kem chi tiet neu co. */
    public static String thanThien(ApiException ex) {
        StringBuilder sb = new StringBuilder(thanThien(ex.code()));
        List<String> details = ex.details();
        if (!details.isEmpty()) {
            sb.append("\n");
            if (ex.code() == ErrorCode.COURSE_NOT_OPEN) {
                sb.append("Há»c pháº§n khÃ´ng má»Ÿ: ");
            } else {
                sb.append("Chi tiáº¿t: ");
            }
            sb.append(details.stream().collect(Collectors.joining(", ")));
        } else if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            sb.append("\n").append(ex.getMessage());
        }
        return sb.toString();
    }
}
