package com.tkb.common.dto;

/**
 * B2.4 - SlotDto: Cau truc du lieu mot buoi hoc trong tuan (Muc 8 Protocol).
 *
 * Cau truc JSON chuan:
 *   {"dayOfWeek": 3, "startPeriod": 1, "periodCount": 2, "room": "C.A207", "weeks": "1234567890--------"}
 *
 * Quy uoc gia tri:
 *   - dayOfWeek: 2 den 8 (2 = Thu Hai, 7 = Thu Bay, 8 = Chu Nhat).
 *   - startPeriod: Tiet bat dau (tu 1; tiet sang: 1-5, tiet chieu: 6-10, ngoai le toi 11-12).
 *   - periodCount: So tiet hoc lien tuc (tiet ket thuc = startPeriod + periodCount - 1 <= 12).
 *   - room: Ma phong hoc (vd: "C.A207", "C.E102").
 *   - weeks: Chuoi bieu dien tuan hoc (vd: "1234567890--------", "-2-4-6-8-0-2------" hoac "********").
 *            Ky tu '-' hoac khoang trang ' ' la tuan NGHI; cac ky tu khac (so hoac '*') la tuan CO HOC.
 *
 * Vi tri dat file:
 *   common/src/main/java/com/tkb/common/dto/SlotDto.java
 */
public class SlotDto {

    private int dayOfWeek;
    private int startPeriod;
    private int periodCount;
    private String room;
    private String weeks;

    public SlotDto() {
    }

    public SlotDto(int dayOfWeek, int startPeriod, int periodCount, String room, String weeks) {
        this.dayOfWeek = dayOfWeek;
        this.startPeriod = startPeriod;
        this.periodCount = periodCount;
        this.room = room;
        this.weeks = weeks != null ? weeks : "";
    }

    /**
     * Tinh tiet ket thuc cua buoi hoc (vd: bat dau tiet 1, hoc 3 tiet -> ket thuc tiet 3).
     */
    public int getEndPeriod() {
        return startPeriod + periodCount - 1;
    }

    /**
     * Kiem tra hai buoi hoc co trung lich voi nhau hay khong (xet ca Thu, Tiet va Tuan hoc - A5).
     * Hai buoi chi bi coi la trung khi:
     *   1. Cung thu trong tuan (dayOfWeek).
     *   2. Giao nhau ve khoang tiet hoc.
     *   3. Co it nhat 1 tuan hoc chung trong chuoi weeks.
     */
    public boolean conflictsWith(SlotDto other) {
        if (other == null || this.dayOfWeek != other.dayOfWeek) {
            return false;
        }
        // Kiem tra giao nhau ve tiet hoc
        boolean periodOverlap = this.startPeriod <= other.getEndPeriod()
                && other.startPeriod <= this.getEndPeriod();
        if (!periodOverlap) {
            return false;
        }
        // Kiem tra giao nhau ve tuan hoc
        return weeksOverlap(this.weeks, other.weeks);
    }

    /**
     * Kiem tra 2 chuoi tuan hoc co tuan nao trung nhau khong.
     * Quy uoc:
     *   - Ky tu '-' hoac ' ' (khoang trang) tai vi tri i la tuan thu (i+1) KHONG hoc.
     *   - Moi ky tu khac ('*', chữ số '1'..'9', '0') tại vị trí i là tuần thứ (i+1) CÓ học.
     *   - Neu 1 trong 2 chuoi rong/null -> mac dinh coi nhu hoc tat ca cac tuan (trung nhau).
     */
    public static boolean weeksOverlap(String w1, String w2) {
        if (w1 == null || w1.isEmpty() || w2 == null || w2.isEmpty()) {
            return true;
        }
        int minLen = Math.min(w1.length(), w2.length());
        for (int i = 0; i < minLen; i++) {
            char c1 = w1.charAt(i);
            char c2 = w2.charAt(i);
            boolean active1 = (c1 != '-' && c1 != ' ');
            boolean active2 = (c2 != '-' && c2 != ' ');
            if (active1 && active2) {
                return true; // Trung nhau tai tuan thu (i + 1)
            }
        }
        return false;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public int getStartPeriod() {
        return startPeriod;
    }

    public void setStartPeriod(int startPeriod) {
        this.startPeriod = startPeriod;
    }

    public int getPeriodCount() {
        return periodCount;
    }

    public void setPeriodCount(int periodCount) {
        this.periodCount = periodCount;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getWeeks() {
        return weeks;
    }

    public void setWeeks(String weeks) {
        this.weeks = weeks;
    }
}