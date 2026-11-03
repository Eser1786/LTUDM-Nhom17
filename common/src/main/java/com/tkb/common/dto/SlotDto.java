package com.tkb.common.dto;

/**
 * B2.4 - SlotDto: Cau truc du lieu mot buoi hoc trong tuan (Muc 8 Protocol).
 *
 * Cau truc JSON chuan:
 *   {
 *     "dayOfWeek": 3,
 *     "startPeriod": 1,
 *     "periodCount": 2,
 *     "room": "C.A207",
 *     "weeks": "1234567890--------",
 *     "type": "LT",
 *     "labGroup": ""
 *   }
 *
 * Quy uoc gia tri:
 *   - dayOfWeek: 2 den 8 (2 = Thu Hai, 7 = Thu Bay, 8 = Chu Nhat).
 *   - startPeriod: Tiet bat dau (tu 1; tiet sang: 1-5, tiet chieu: 6-10, ngoai le toi 11-12).
 *   - periodCount: So tiet hoc lien tuc (tiet ket thuc = startPeriod + periodCount - 1 <= 12).
 *   - room: Ma phong hoc (vd: "C.A207", "C.E102").
 *   - weeks: Chuoi bieu dien tuan hoc (vd: "1234567890--------", "-2-4-6-8-0-2------" hoac "********").
 *   - type: "LT" (Ly thuyet) hoac "TH" (Thuc hanh).
 *   - labGroup: So to thuc hanh ("" neu la LT, "01", "02"... neu la TH).
 *   - weekMask: Mat na bit 62-bit (bit i = 1 neu tuan thu i+1 co hoc) giup kiem tra trung tuan cuc nhanh.
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
    private String type = "LT";
    private String labGroup = "";
    private long weekMask;

    public SlotDto() {
    }

    public SlotDto(int dayOfWeek, int startPeriod, int periodCount, String room, String weeks) {
        this(dayOfWeek, startPeriod, periodCount, room, weeks, "LT", "", computeWeekMask(weeks));
    }

    public SlotDto(
            int dayOfWeek,
            int startPeriod,
            int periodCount,
            String room,
            String weeks,
            String type,
            String labGroup,
            long weekMask) {
        this.dayOfWeek = dayOfWeek;
        this.startPeriod = startPeriod;
        this.periodCount = periodCount;
        this.room = room;
        this.weeks = weeks != null ? weeks : "";
        this.type = (type != null && !type.isBlank()) ? type : "LT";
        this.labGroup = labGroup != null ? labGroup : "";
        this.weekMask = (weekMask > 0) ? weekMask : computeWeekMask(this.weeks);
    }

    /**
     * Tinh tiet ket thuc cua buoi hoc (vd: bat dau tiet 1, hoc 3 tiet -> ket thuc tiet 3).
     */
    public int getEndPeriod() {
        return startPeriod + periodCount - 1;
    }

    /**
     * Kiem tra hai buoi hoc co trung lich voi nhau hay khong (xet ca Thu, Tiet va Tuan hoc - A5).
     */
    public boolean conflictsWith(SlotDto other) {
        if (other == null || this.dayOfWeek != other.dayOfWeek) {
            return false;
        }
        boolean periodOverlap = this.startPeriod <= other.getEndPeriod()
                && other.startPeriod <= this.getEndPeriod();
        if (!periodOverlap) {
            return false;
        }
        if (this.weekMask > 0 && other.weekMask > 0) {
            return (this.weekMask & other.weekMask) != 0L;
        }
        return weeksOverlap(this.weeks, other.weeks);
    }

    /**
     * Chuyen chuoi tuan hoc (weeks) sang mat na bit 62-bit (tuan_mask trong MySQL).
     * Ky tu '-', '_', '.', hoac khoang trang ' ' la tuan NGHI (bit = 0).
     * Moi ky tu khac (so hoac '*') la tuan CO HOC (bit = 1).
     */
    public static long computeWeekMask(String weeks) {
        if (weeks == null || weeks.isEmpty()) {
            return (1L << 15) - 1L; // Mac dinh 15 tuan dau (32767)
        }
        int limit = Math.min(weeks.length(), 62);
        long mask = 0L;
        for (int i = 0; i < limit; i++) {
            char c = weeks.charAt(i);
            if (c != '-' && c != ' ' && c != '_' && c != '.') {
                mask |= (1L << i);
            }
        }
        return mask > 0 ? mask : ((1L << 15) - 1L);
    }

    /**
     * Kiem tra 2 chuoi tuan hoc co tuan nao trung nhau khong.
     */
    public static boolean weeksOverlap(String w1, String w2) {
        return (computeWeekMask(w1) & computeWeekMask(w2)) != 0L;
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
        this.weeks = weeks != null ? weeks : "";
        if (this.weekMask <= 0) {
            this.weekMask = computeWeekMask(this.weeks);
        }
    }

    public String getType() {
        return type != null ? type : "LT";
    }

    public void setType(String type) {
        this.type = (type != null && !type.isBlank()) ? type : "LT";
    }

    public String getLabGroup() {
        return labGroup != null ? labGroup : "";
    }

    public void setLabGroup(String labGroup) {
        this.labGroup = labGroup != null ? labGroup : "";
    }

    public long getWeekMask() {
        if (weekMask <= 0) {
            weekMask = computeWeekMask(weeks);
        }
        return weekMask;
    }

    public void setWeekMask(long weekMask) {
        this.weekMask = weekMask;
    }
}