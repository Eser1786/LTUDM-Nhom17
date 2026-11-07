package com.tkb.frontend.user.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public final class Types {

    private Types() {
    }

    public record Semester(String ma, String ten,
                           @JsonAlias({"current", "dang_dung", "dangDung"}) boolean dangDung,
                           String ngayBatDau, String ngayKetThuc) {
        @Override
        public String toString() {
            return ten + (dangDung ? " (hiện tại)" : "");
        }
    }

    public record Student(String hoTen, String mssv, String lop, String email) {
        public String chuDau() {
            String[] tu = hoTen.trim().split("\\s+");
            if (tu.length == 0 || tu[0].isBlank()) {
                return "?";
            }
            if (tu.length == 1) {
                return Character.toString(tu[0].charAt(0)).toUpperCase();
            }
            return (Character.toString(tu[tu.length - 2].charAt(0))
                    + Character.toString(tu[tu.length - 1].charAt(0))).toUpperCase();
        }
    }

    public record Course(
            @JsonAlias({"ma_mh", "maMH", "maMon", "ma_mon"}) String maMh,
            @JsonAlias({"ten_hp", "tenHocPhan", "ten_mon", "tenMon", "tenHocPhan"}) String ten,
            @JsonAlias({"so_tc", "soTinChi", "tinChi", "credits", "credit"}) int soTc) {
    }

    public record Section(int id,
                          @JsonAlias({"ma_hoc_ky", "maHocKy"}) String maHocKy,
                          @JsonAlias({"ma_mh", "maMH", "maMon"}) String maMh,
                          @JsonAlias({"nhom_lop", "nhomLop"}) String nhom,
                          @JsonAlias({"to_th", "toTh"}) String toTh,
                          @JsonAlias({"loai_lop", "loaiLop"}) String loai,
                          @JsonAlias({"si_so", "siSo"}) Integer siSo,
                          @JsonAlias({"giang_vien", "giangVien"}) String giangVien) {
    }

    public record ClassSession(int thu, int tietBd, int soTiet, String phong) {
    }

    public record SectionSlot(Section section,
                              @JsonAlias({"ten_hp", "tenHocPhan", "ten_mon"}) String tenHocPhan,
                              @JsonAlias({"so_tc", "soTinChi", "tinChi", "credits", "credit"}) int soTc,
                              List<ClassSession> sessions) {
        public boolean trung(SectionSlot other) {
            for (ClassSession a : sessions) {
                for (ClassSession b : other.sessions) {
                    if (a.thu() != b.thu()) {
                        continue;
                    }
                    int aEnd = a.tietBd() + a.soTiet() - 1;
                    int bEnd = b.tietBd() + b.soTiet() - 1;
                    if (a.tietBd() <= bEnd && b.tietBd() <= aEnd) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public record Criteria(int uuTienSang, int itNgay, int khoangTrong, int ngayYeuThich,
                            List<Integer> ngayChon, int soPhuongAn,
                            String timeMode, List<Integer> preferredDays, List<Integer> preferredSlotsMask,
                            String preferredCampus, boolean avoidCampusSwitch,
                            java.util.Map<String, String> preferredLecturers,
                            java.util.Map<String, String> lockedGroups) {
        public Criteria(int uuTienSang, int itNgay, int khoangTrong, int ngayYeuThich,
                        List<Integer> ngayChon, int soPhuongAn) {
            this(uuTienSang, itNgay, khoangTrong, ngayYeuThich, ngayChon, soPhuongAn,
                    "MORNING", List.of(2, 3, 4, 5, 6), null, "C", true,
                    new java.util.LinkedHashMap<>(), new java.util.LinkedHashMap<>());
        }

        public static Criteria macDinh() {
            return new Criteria(3, 3, 3, 0, List.of(2, 3, 4, 5, 6), 3);
        }
    }

    public record PlanRequest(String maHocKy, List<String> maMhTheoUuTien, Criteria criteria,
                              List<Integer> sectionIds) {
    }

    public record OptionStats(int soNgayHoc, int soTietTrong, int soTietSang, int tongTiet) {
        public int phutTrong() {
            return soTietTrong * 60;
        }

        public int tyLeSang() {
            return tongTiet <= 0 ? 0 : (int) Math.round(soTietSang * 100.0 / tongTiet);
        }
    }

    public record ScheduleOption(int rank, String ten, double diem, OptionStats stats, String moTa,
                                 List<SectionSlot> slots, List<String> biLoai,
                                 List<SectionSlot> nhomThayThe) {
    }

    public record JobResult(String maHocKy, List<ScheduleOption> options) {
    }

    public record JobStatus(String jobId, State state, int phanTram, JobResult ketQua,
                            String thongBaoLoi) {
        public enum State {RUNNING, DONE, CANCELLED, FAILED}
    }

    public record SavedPlan(long id, String maHocKy, String ten, double diem, OptionStats stats,
                            String ngayTao, int soHocPhan, int soTinChi, int soBuoi) {
    }

    public record PlanDetail(long id, String maHocKy, String ten, double diem, OptionStats stats,
                             List<SectionSlot> slots, List<String> biLoai, String ngayTao) {
        public int soHocPhan() {
            return (int) slots.stream().map(s -> s.section().maMh()).distinct().count();
        }

        public int soTinChi() {
            java.util.Set<String> daTinh = new java.util.HashSet<>();
            int tc = 0;
            for (SectionSlot s : slots) {
                if (daTinh.add(s.section().maMh())) {
                    tc += s.soTc();
                }
            }
            return tc;
        }

        public int soBuoi() {
            int n = 0;
            for (SectionSlot s : slots) {
                n += s.sessions().size();
            }
            return n;
        }
    }
}