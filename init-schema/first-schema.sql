-- Schema CSDL cho đồ án "Tối ưu thời khóa biểu"
-- Được mount vào /docker-entrypoint-initdb.d nên chỉ chạy tự động ở lần khởi động đầu tiên
-- (khi volume MySQL còn trống).
--
-- Nguyên tắc thiết kế:
--   * Dữ liệu danh mục dùng chung (hoc_phan, nguoi_dung) không phụ thuộc học kỳ.
--   * Mọi dữ liệu thay đổi theo học kỳ đều mang mã học kỳ, nên học kỳ mới chỉ THÊM dòng
--     và học kỳ cũ vẫn xem lại được.
--   * Nhóm lớp biến mất khỏi file import mới sẽ bị tắt (dang_hoat_dong = FALSE), không xóa,
--     để các phương án đã lưu vẫn dùng được.
--   * Không phân biệt lý thuyết / thực hành: một nhóm lớp là MỘT đơn vị chọn, gồm mọi buổi học
--     của nhóm đó (cả buổi LT lẫn buổi TH).
--   * Mỗi buổi học có mặt nạ tuần (tuan_mask) để kiểm tra trùng lịch theo từng tuần.
--   * Quy ước tiết: 1-5 là buổi sáng, 6-10 là buổi chiều. Thứ: 2 = Thứ hai ... 8 = Chủ nhật.

SET NAMES utf8mb4;

-- ---------------------------------------------------------------
-- 1. DỮ LIỆU NHÓM LỚP (nạp từ sổ tay đăng ký môn học)
-- ---------------------------------------------------------------

CREATE TABLE hoc_ky (
  ma            VARCHAR(15)  NOT NULL,                -- vd '2025-2026-3'
  ten           VARCHAR(100) NOT NULL,                -- vd 'Học kỳ 3 - Năm học 2025-2026'
  dang_dung     BOOLEAN      NOT NULL DEFAULT FALSE,  -- học kỳ hiện tại để sinh viên xếp lịch
  -- Cột sinh tự động: bằng 1 khi là học kỳ hiện tại, ngược lại là NULL. Khóa UNIQUE cho phép
  -- nhiều NULL nhưng chỉ một giá trị 1, nên tối đa MỘT học kỳ được là học kỳ hiện tại.
  co_hien_tai   TINYINT GENERATED ALWAYS AS (IF(dang_dung, 1, NULL)) STORED,
  PRIMARY KEY (ma),
  UNIQUE KEY uq_hoc_ky_hien_tai (co_hien_tai)
) ENGINE=InnoDB;
-- Đổi học kỳ: đặt dòng cũ về FALSE trước, rồi mới đặt dòng mới, trong cùng một transaction.

CREATE TABLE hoc_phan (
  ma_mh  VARCHAR(10)  NOT NULL,                       -- vd '841109'
  ten    VARCHAR(150) NOT NULL,
  so_tc  TINYINT UNSIGNED NOT NULL,
  PRIMARY KEY (ma_mh)
) ENGINE=InnoDB;

-- Một dòng = một nhóm của một học phần trong một học kỳ (vd nhóm 01 của môn Cơ sở dữ liệu).
-- Nhóm gồm TẤT CẢ buổi học trong bảng lich_hoc, kể cả các buổi thực hành.
CREATE TABLE nhom_lop (
  id               INT UNSIGNED NOT NULL AUTO_INCREMENT,
  ma_hoc_ky        VARCHAR(15)  NOT NULL,
  ma_mh            VARCHAR(10)  NOT NULL,
  ten_mh           VARCHAR(150) NOT NULL,
  nhom             VARCHAR(5)   NOT NULL,
  si_so            INT UNSIGNED NULL,
  giang_vien       VARCHAR(100) NULL,                 -- môn Thực tập tốt nghiệp không có giảng viên
  ma_vc            VARCHAR(10)  NULL,
  ten_lop          VARCHAR(30)  NULL,
  -- FALSE khi một lần import sau không còn liệt kê nhóm này. Thuật toán xếp lịch chỉ đọc
  -- nhóm đang hoạt động; phương án đã lưu vẫn hiển thị được nhóm đã tắt.
  dang_hoat_dong   BOOLEAN      NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id),
  UNIQUE KEY uq_nhom_lop (ma_hoc_ky, ma_mh, nhom),
  -- Cho phép bảng khác tham chiếu (id, ma_hoc_ky) để không thể lệch học kỳ
  UNIQUE KEY uq_nhom_lop_id_hoc_ky (id, ma_hoc_ky),
  KEY idx_nhom_lop_tra_cuu (ma_hoc_ky, dang_hoat_dong, ma_mh),
  CONSTRAINT fk_nhom_lop_hoc_ky   FOREIGN KEY (ma_hoc_ky) REFERENCES hoc_ky(ma),
  CONSTRAINT fk_nhom_lop_hoc_phan FOREIGN KEY (ma_mh)     REFERENCES hoc_phan(ma_mh)
) ENGINE=InnoDB;

-- Một nhóm có nhiều buổi học (vd nhóm 01 học thứ 2, tiết 1-5 và thứ 4, tiết 1-5).
-- Khi import lại một nhóm: xóa các lịch cũ của nhóm rồi chèn lại, trong cùng một transaction.
CREATE TABLE lich_hoc (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nhom_lop_id  INT UNSIGNED NOT NULL,
  thu          TINYINT UNSIGNED NOT NULL,             -- 2..8 (2 = Thứ hai ... 7 = Thứ bảy, 8 = Chủ nhật)
  tiet_bd      TINYINT UNSIGNED NOT NULL,
  so_tiet      TINYINT UNSIGNED NOT NULL,
  phong        VARCHAR(20) NULL,
  -- to_th        VARCHAR(5)  NOT NULL DEFAULT '',       -- chỉ để hiển thị: '' hoặc số tổ thực hành trong PDF
  -- tuan_hoc     VARCHAR(30) NULL,                      -- chuỗi dấu * nguyên bản trong PDF
  -- Mặt nạ tuần: bit 0 = tuần 1, bit 1 = tuần 2, ... (tối đa 62 tuần, dùng BIGINT có dấu cho dễ
  -- đọc bằng JDBC). Hai buổi chỉ trùng nhau khi cùng thứ, giao nhau về tiết VÀ (mask_a & mask_b) <> 0.
  -- tuan_mask    BIGINT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_lich_hoc_nhom (nhom_lop_id),
  CONSTRAINT fk_lich_hoc_nhom FOREIGN KEY (nhom_lop_id)
    REFERENCES nhom_lop(id) ON DELETE CASCADE,
  CONSTRAINT ck_lich_thu CHECK (thu BETWEEN 2 AND 8),
  -- Tiết 1-5 buổi sáng, tiết 6-10 buổi chiều; một buổi không được vượt quá tiết 10
  CONSTRAINT ck_lich_tiet CHECK (
    tiet_bd >= 1 AND so_tiet >= 1 AND tiet_bd + so_tiet - 1 <= 10
  )
  -- Mặt nạ tuần: bit 0 = tuần 1, bit 1 = tuần 2, ... (tối đa 62 tuần, dùng BIGINT có dấu cho dễ
  -- đọc bằng JDBC). Hai buổi chỉ trùng nhau khi cùng thứ, giao nhau về tiết VÀ (mask_a & mask_b) <> 0.
  -- CONSTRAINT ck_lich_tuan CHECK (tuan_mask > 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------
-- 2. TÀI KHOẢN VÀ PHƯƠNG ÁN ĐÃ LƯU
-- ---------------------------------------------------------------

CREATE TABLE nguoi_dung (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  ten_dang_nhap  VARCHAR(50)  NOT NULL,
  mat_khau_hash  VARCHAR(255) NOT NULL,               -- chuỗi hash (BCrypt), không lưu mật khẩu rõ
  vai_tro        ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
  ngay_tao       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_nguoi_dung_ten (ten_dang_nhap)
) ENGINE=InnoDB;

CREATE TABLE phuong_an (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nguoi_dung_id    BIGINT UNSIGNED NOT NULL,
  ma_hoc_ky        VARCHAR(15) NOT NULL,
  ten              VARCHAR(100) NOT NULL,
  tieu_chi         JSON NULL,                         -- tiêu chí và trọng số đã dùng để sinh phương án
  diem             DECIMAL(7,2) NULL,
  so_ngay_hoc      TINYINT UNSIGNED NULL,             -- các chỉ số tính sẵn để so sánh
  so_tiet_trong    SMALLINT UNSIGNED NULL,
  so_tiet_sang     SMALLINT UNSIGNED NULL,
  ngay_tao         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_phuong_an_ten (nguoi_dung_id, ten),
  UNIQUE KEY uq_phuong_an_id_hoc_ky (id, ma_hoc_ky),
  KEY idx_phuong_an_nguoi_dung_hoc_ky (nguoi_dung_id, ma_hoc_ky),
  CONSTRAINT fk_phuong_an_nguoi_dung FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id) ON DELETE CASCADE,
  CONSTRAINT fk_phuong_an_hoc_ky     FOREIGN KEY (ma_hoc_ky)     REFERENCES hoc_ky(ma)
) ENGINE=InnoDB;

-- Các nhóm lớp được chọn trong một phương án.
-- ma_hoc_ky được lặp lại ở đây có chủ đích: cả hai khóa ngoại ghép đều dùng nó, nên chính CSDL
-- bảo đảm một phương án chỉ chứa nhóm lớp thuộc đúng học kỳ của phương án đó.
-- RESTRICT: nhóm lớp nằm trong phương án đã lưu thì không xóa được (hãy tắt dang_hoat_dong).
CREATE TABLE phuong_an_chi_tiet (
  phuong_an_id  BIGINT UNSIGNED NOT NULL,
  nhom_lop_id   INT UNSIGNED NOT NULL,
  ma_hoc_ky     VARCHAR(15) NOT NULL,
  PRIMARY KEY (phuong_an_id, nhom_lop_id),
  KEY idx_pact_nhom_lop (nhom_lop_id),
  CONSTRAINT fk_pact_phuong_an FOREIGN KEY (phuong_an_id, ma_hoc_ky)
    REFERENCES phuong_an(id, ma_hoc_ky) ON DELETE CASCADE,
  CONSTRAINT fk_pact_nhom_lop FOREIGN KEY (nhom_lop_id, ma_hoc_ky)
    REFERENCES nhom_lop(id, ma_hoc_ky) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Học phần bị loại khỏi phương án do trùng lịch
CREATE TABLE phuong_an_bi_loai (
  phuong_an_id  BIGINT UNSIGNED NOT NULL,
  ma_mh         VARCHAR(10) NOT NULL,
  PRIMARY KEY (phuong_an_id, ma_mh),
  CONSTRAINT fk_pabl_phuong_an FOREIGN KEY (phuong_an_id) REFERENCES phuong_an(id) ON DELETE CASCADE,
  CONSTRAINT fk_pabl_hoc_phan  FOREIGN KEY (ma_mh) REFERENCES hoc_phan(ma_mh)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------
-- 3. DỮ LIỆU KHỞI TẠO
-- ---------------------------------------------------------------
INSERT INTO hoc_ky (ma, ten, dang_dung)
VALUES ('2026-2027-1', 'Học kỳ 1 - Năm học 2026-2027', TRUE);