package com.tkb.frontend.user.model;

import java.util.List;

/**
 * Cac DTO trao doi giua client va server.
 * Moi record la mot doi tuong bat bien, khong phu thuoc Swing.
 */
public final class Types {

    private Types() {
    }

    /** hoc_ky */
    public record Semester(String ma, String ten, boolean dangDung,
                           String ngayBatDau, String ngayKetThuc) {
        @Override
        public String toString() {
            return ten + (dangDung ? " (hiện tại)" : "");
        }
    }

    /** thong tin sinh vien hien tai (dung cho header va man hinh dang nhap). */
    public record Student(String hoTen, String mssv, String lop, String email) {
        /** Hai chu dau, vi du "Nguyen Thao Minh Anh" -> "MA". */
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

    /** hoc_phan */
    public record Course(String maMh, String ten, int soTc) {
    }

    /** nhom_lop - mot nhom ly thuyet (toTh = "") hoac mot to thuc hanh (toTh = "01"...). */
    public record Section(int id, String maHocKy, String maMh, String nhom, String toTh,
                          String loai, Integer siSo, String giangVien) {
        public String hienThi() {
            return maMh + "." + nhom + ("TH".equals(loai) ? "." + toTh : "") + " (" + loai + ")";
        }
    }

    /** lich_hoc - mot buoi hoc cua mot nhom lop. */
    public record ClassSession(int thu, int tietBd, int soTiet, String phong) {
    }

    /** Nhom lop kem lich hoc va thong tin hoc phan, dung de ve luoi. */
    public record SectionSlot(Section section, String tenHocPhan, int soTc,
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

    /**
     * Tieu chi xep lich do nguoi dung chon.
     * Moi tieu chi la con so 0..5 tren thanh keo (5 = quan trong nhat);
     * ngayChon la cac thu da chon (2..7), rong nghia la khong yeu cau ngay cu the.
     */
    public record Criteria(int uuTienSang, int itNgay, int khoangTrong, int ngayYeuThich,
                           List<Integer> ngayChon, int soPhuongAn) {
        public static Criteria macDinh() {
            return new Criteria(3, 3, 3, 0, List.of(2, 3, 4, 5, 6), 3);
        }
    }

    /**
     * Yeu cau xep lich.
     *
     * @param maHocKy       hoc ky
     * @param maMhTheoUuTien danh sach ma hoc phan, phan tu dau uu tien cao nhat
     * @param sectionIds    cac nhom lop da chon cu the; rong nghia la chon tu do
     */
    public record PlanRequest(String maHocKy, List<String> maMhTheoUuTien, Criteria criteria,
                              List<Integer> sectionIds) {
    }

    /** Chi so cua mot phuong an. soTietTrong dem cac o trong giua 2 buoi hoc trong ngay. */
    public record OptionStats(int soNgayHoc, int soTietTrong, int soTietSang, int tongTiet) {
        /** So phut nghi giua cac buoi hoc. */
        public int phutTrong() {
            return soTietTrong * 60;
        }

        /** Ty le phan tram tiet hoc buoi sang (tiet 1..5). */
        public int tyLeSang() {
            return tongTiet <= 0 ? 0 : (int) Math.round(soTietSang * 100.0 / tongTiet);
        }
    }

    /** Mot phuong an xep lich do thuat toan sinh ra. */
    public record ScheduleOption(int rank, String ten, double diem, OptionStats stats, String moTa,
                                 List<SectionSlot> slots, List<String> biLoai,
                                 List<SectionSlot> nhomThayThe) {
    }

    /** Ket qua cua mot job xep lich. */
    public record JobResult(String maHocKy, List<ScheduleOption> options) {
    }

    /** Trang thai job. */
    public record JobStatus(String jobId, State state, int phanTram, JobResult ketQua,
                            String thongBaoLoi) {
        public enum State {RUNNING, DONE, CANCELLED, FAILED}
    }

    /** Mot phuong an da luu (khong kem chi tiet). */
    public record SavedPlan(long id, String maHocKy, String ten, double diem, OptionStats stats,
                            String ngayTao, int soHocPhan, int soTinChi, int soBuoi) {
    }

    /** Phuong an da luu kem chi tiet de xem lai. */
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
