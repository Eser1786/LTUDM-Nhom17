#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script boc tach du lieu Thoi Khoa Bieu tu cac file PDF (Chuyen nganh & Mon chung),
tach o lon To_TH (Thuc hanh) cho tung dong tiet hoc, giu nguyen khoang trang dau
cua chuoi Tuan_hoc (vd: "  *" = hoc tuan 3), phan loai LT/TH, xuat ra CSV
va nap truc tiep vao MySQL trong Docker.

Vi tri dat file:
    tools/processing_data.py (hoac thu muc goc cua du an)
"""

import os
import re
import sys
from pathlib import Path
from typing import Dict, List, Optional, Set, Tuple

import pandas as pd
import pdfplumber

try:
    import mysql.connector
except ImportError:
    mysql = None

# ==============================================================================
# 1. CAU HINH DANH SACH FILE PDF & COT DU LIEU
# ==============================================================================
courses = [
    "DKD", "DCM", "DCV", "DCT", "DDL", "DGD", "DGM", "DGT", "DKE", "DDU",
    "DKM", "DTT", "DKQ", "DKI", "DDE", "DDV", "DKP", "DLU", "DAN", "DQG",
    "DQK", "DHA", "DQV", "DQT", "DNH", "DDI", "DHO", "DKH", "DSU", "DLD",
    "DMI", "DVA", "DSI", "DSA", "DTO", "DLI", "DTN", "DTL", "DTU", "DNT",
    "DVI", "DLS", "DDH",
]

semester = "2026-2027-1"
pdf_dir = f"pdf/{semester}/"
output_csv_path = f"csv/{semester}/thoi_khoa_bieu.csv"

# Cau truc 15 cot cua file PDF Chuyen nganh
chuyen_nganh_full = [
    "STT", "Ma_MH", "Ten_mon", "So_TC", "Si_so", "Ho_ten_GV",
    "Ma_VC", "Nhom", "To_TH", "Thu", "TietBD", "So_tiet",
    "Ma_phong", "Ten_lop", "Tuan_hoc",
]
fill_chuyen_nganh_cols = ["Ma_MH", "Ten_mon", "So_TC", "Si_so", "Ho_ten_GV", "Ma_VC", "Nhom"]

# Cau truc 12 cot cua file PDF Mon chung
mon_chung_full = [
    "Ma_VC", "Ho_ten_GV", "Ma_MH", "Nhom", "To_TH", "Ten_mon",
    "Si_so", "Thu", "TietBD", "So_tiet", "Ma_phong", "Tuan_hoc",
]
fill_mon_chung_cols = ["Ma_MH", "Ten_mon", "Si_so", "Ho_ten_GV", "Ma_VC", "Nhom"]

# Cac cot giu lai khi xuat ra CSV (da them So_TC, To_TH, Loai, Tuan_hoc)
keep_cols = [
    "Ma_MH", "Ten_mon", "So_TC", "Ho_ten_GV", "Nhom",
    "To_TH", "Loai", "Thu", "TietBD", "So_tiet", "Ma_phong", "Tuan_hoc",
]

chuyen_nganh_paths = [os.path.join(pdf_dir, f"{c}.pdf") for c in courses]
mon_chung_paths = [os.path.join(pdf_dir, "TKB_monchung.pdf")]


# ==============================================================================
# 2. HAM BOC TACH BẢNG PDF: TACH O LON TO_TH THEO TUNG DONG & GIU KHOANG TRANG TUAN_HOC
# ==============================================================================
def _char_in_bbox(char: dict, bbox: Tuple[float, float, float, float]) -> bool:
    """Kiem tra tam cua ky tu co nam trong khung bbox (x0, top, x1, bottom) hay khong."""
    v_mid = (char["top"] + char["bottom"]) / 2.0
    h_mid = (char["x0"] + char["x1"]) / 2.0
    x0, top, x1, bottom = bbox
    return (h_mid >= x0) and (h_mid < x1) and (v_mid >= top) and (v_mid < bottom)


def _extract_raw_chars_preserve_spaces(cell_chars: List[dict], cell_bbox: Tuple[float, float, float, float]) -> str:
    """
    Ghep cac ky tu trong o Tuan_hoc theo thu tu tu trai sang phai (x0),
    GIU NGUYEN khoang trang ' ' o dau va giua chuoi (vd: '  *' = hoc tuan 3).
    """
    if not cell_chars:
        return ""

    # Sap xep ky tu theo dong (top) roi theo vi tri ngang (x0)
    sorted_chars = sorted(cell_chars, key=lambda c: (round(c["top"], 1), c["x0"]))
    text = "".join(c.get("text", "") for c in sorted_chars)

    # Chi xoa xuong dong va khoang trang thua o CUOI chuoi, giu nguyen khoang trang DAU chuoi
    return text.replace("\r", "").replace("\n", "").rstrip(" ")


def _find_column_x_bounds(table, col_idx: int) -> Optional[Tuple[float, float]]:
    """Tim toa do ngang (x0, x1) cua mot cot dua tren cac cell khong None trong bang."""
    for row in table.rows:
        if col_idx < len(row.cells) and row.cells[col_idx] is not None:
            cell = row.cells[col_idx]
            return (cell[0], cell[2])
    return None


def extract_rows(pdf_path: str, subject_type: str) -> List[List[Optional[str]]]:
    """
    Doc tat ca cac trang trong 1 file PDF va tra ve danh sach cac dong.
    Dac biet:
      1. Cot To_TH trong PDF la 1 o lon gop doc (merged cell) cho ca nhom -> pdfplumber mac dinh
         gan cell=None cho cac dong tiet hoc ben duoi, lam mat dau 'X' nam o cac dong duoi.
         Ham nay cat ngang cot To_TH theo tung dong tiet hoc (row.bbox) de bat chinh xac
         dau 'X' (hoac ma to TH) cho tung buoi hoc!
      2. Cot Tuan_hoc duoc doc truc tiep tu page.chars de giu nguyen khoang trang dau chuoi ('  *').
    """
    if not os.path.exists(pdf_path):
        print(f"[BO QUA] Khong tim thay file: {pdf_path}")
        return []

    if subject_type == "chuyen_nganh":
        to_th_idx = chuyen_nganh_full.index("To_TH")      # index 8
        tuan_hoc_idx = chuyen_nganh_full.index("Tuan_hoc")  # index 14
        ma_mh_idx = chuyen_nganh_full.index("Ma_MH")      # index 1
        nhom_idx = chuyen_nganh_full.index("Nhom")        # index 7
    else:
        to_th_idx = mon_chung_full.index("To_TH")         # index 4
        tuan_hoc_idx = mon_chung_full.index("Tuan_hoc")   # index 11
        ma_mh_idx = mon_chung_full.index("Ma_MH")         # index 2
        nhom_idx = mon_chung_full.index("Nhom")           # index 3

    all_rows: List[List[Optional[str]]] = []
    detected_to_th_count = 0

    with pdfplumber.open(pdf_path) as pdf:
        for page_num, page in enumerate(pdf.pages, start=1):
            tables = page.find_tables()
            if not tables:
                continue

            page_chars = page.chars

            for table in tables:
                raw_rows = table.extract()
                if not raw_rows:
                    continue

                to_th_x_bounds = _find_column_x_bounds(table, to_th_idx)
                tuan_x_bounds = _find_column_x_bounds(table, tuan_hoc_idx)

                # Theo doi pham vi cac dong thuoc cung 1 o lon To_TH (merged cell)
                # de xu ly truong hop dau 'X' nam giua o lon
                current_group_indices: List[int] = []
                current_merged_to_th_bbox: Optional[Tuple[float, float, float, float]] = None

                for r_idx, row_obj in enumerate(table.rows):
                    if r_idx >= len(raw_rows):
                        break
                    row_vals = list(raw_rows[r_idx])
                    row_top, row_bottom = row_obj.bbox[1], row_obj.bbox[3]

                    # Loc tat ca ky tu nam trong dai ngang cua dong hien tai
                    row_chars = [
                        c for c in page_chars
                        if row_top <= ((c["top"] + c["bottom"]) / 2.0) < row_bottom
                    ]

                    # --- A. TACH O LON TO_TH CHO DONG TIET HOC HIEN TAI ---
                    if to_th_x_bounds is not None and to_th_idx < len(row_vals):
                        # Cat khung (col_x0, row_top, col_x1, row_bottom) cho dong nay
                        virtual_to_th_cell = (to_th_x_bounds[0], row_top, to_th_x_bounds[1], row_bottom)
                        to_th_chars = [c for c in row_chars if _char_in_bbox(c, virtual_to_th_cell)]
                        sliced_to_th = "".join(
                            c.get("text", "") for c in sorted(to_th_chars, key=lambda c: c["x0"])
                        ).strip()

                        row_vals[to_th_idx] = sliced_to_th
                        if sliced_to_th and sliced_to_th not in ("TH", "Tổ TH", "Tổ\nTH"):
                            detected_to_th_count += 1

                    # --- B. DOC COT TUAN_HOC GIU NGUYEN KHOANG TRANG DAU ---
                    if tuan_x_bounds is not None and tuan_hoc_idx < len(row_vals):
                        virtual_tuan_cell = (tuan_x_bounds[0], row_top, tuan_x_bounds[1], row_bottom)
                        tuan_chars = [c for c in row_chars if _char_in_bbox(c, virtual_tuan_cell)]
                        raw_tuan = _extract_raw_chars_preserve_spaces(tuan_chars, virtual_tuan_cell)
                        if raw_tuan:
                            row_vals[tuan_hoc_idx] = raw_tuan

                    # --- C. KIEM TRA NEU O LON TO_TH CO KY TU NAM NGAY BIEN GIUA 2 DONG ---
                    # Neu cell goc cua To_TH tai dong dau nhom la 1 o lon (top..bottom bao trum nhieu dong)
                    orig_cell = row_obj.cells[to_th_idx] if to_th_idx < len(row_obj.cells) else None
                    if orig_cell is not None:
                        # Bat dau 1 o To_TH moi
                        _flush_merged_to_th_fallback(
                            all_rows, current_group_indices, current_merged_to_th_bbox,
                            page_chars, to_th_idx
                        )
                        current_group_indices = [len(all_rows)]
                        current_merged_to_th_bbox = orig_cell
                    else:
                        if current_merged_to_th_bbox is not None:
                            current_group_indices.append(len(all_rows))

                    all_rows.append(row_vals)

                # Flush nhom cuoi cung cua bang
                _flush_merged_to_th_fallback(
                    all_rows, current_group_indices, current_merged_to_th_bbox,
                    page_chars, to_th_idx
                )

    if detected_to_th_count > 0:
        print(f"[INFO] {os.path.basename(pdf_path)}: Phat hien {detected_to_th_count} dong co danh dau o To_TH.")
    return all_rows


def _flush_merged_to_th_fallback(
    all_rows: List[List[Optional[str]]],
    group_indices: List[int],
    merged_bbox: Optional[Tuple[float, float, float, float]],
    page_chars: List[dict],
    to_th_idx: int,
) -> None:
    """
    Xu ly truong hop o lon To_TH co chua ky tu (vd 1 dau 'X' nam giua o lon),
    nhung do nam ngay tren duong bien hoac nhom co nhieu dong nen can phan bo:
      - Neu bat ky dong nao trong nhom da nhan duoc dau 'X' theo toa do Y -> giu nguyen.
      - Neu o lon co text (vd 'X') ma chua dong nao nhan duoc (do nam lech bien) ->
        gan vao cac dong cua nhom do.
    """
    if not group_indices or merged_bbox is None:
        return

    # Kiem tra xem da co dong nao trong nhom bat duoc text To_TH chua
    already_captured = any(
        bool((all_rows[idx][to_th_idx] or "").strip()) for idx in group_indices
    )
    if already_captured:
        return

    # Lay toan bo ky tu nam trong o lon merged_bbox
    merged_chars = [c for c in page_chars if _char_in_bbox(c, merged_bbox)]
    full_text = "".join(
        c.get("text", "") for c in sorted(merged_chars, key=lambda c: (round(c["top"], 1), c["x0"]))
    ).strip()

    if not full_text or full_text in ("TH", "Tổ TH"):
        return

    # Neu o lon co text ma chua gan vao dong nao -> gan cho cac dong trong nhom
    lines = [line.strip() for line in full_text.splitlines() if line.strip()]
    if len(lines) == len(group_indices):
        for idx, val in zip(group_indices, lines):
            all_rows[idx][to_th_idx] = val
    else:
        for idx in group_indices:
            all_rows[idx][to_th_idx] = lines[0]


# ==============================================================================
# 3. CHUYEN BANG PDF THANH DATAFRAME & PHAN LOAI LT / TH
# ==============================================================================
def determine_loai_and_to_th(row: pd.Series) -> Tuple[str, str]:
    """
    Neu o To_TH co noi dung (vd 'X', 'x', '01', '1'...) -> Loai = 'TH'.
    Neu o To_TH trong -> Loai = 'LT'.
    """
    raw_to_th = str(row.get("To_TH") or "").strip()
    if raw_to_th and raw_to_th.lower() not in ("none", "nan", "th", "tổ th"):
        return raw_to_th, "TH"
    return "", "LT"


def load_pdf(pdf_path: str, subject_type: str) -> pd.DataFrame:
    rows = extract_rows(pdf_path, subject_type)
    if not rows:
        return pd.DataFrame(columns=keep_cols)

    if subject_type == "chuyen_nganh":
        df = pd.DataFrame(rows, columns=chuyen_nganh_full)
        # Chi ffill cac cot thong tin chung cua nhom, KHONG ffill To_TH vi To_TH da duoc cat theo tung dong
        df[fill_chuyen_nganh_cols] = df[fill_chuyen_nganh_cols].replace("", None).ffill()
    elif subject_type == "mon_chung":
        df = pd.DataFrame(rows, columns=mon_chung_full)
        df[fill_mon_chung_cols] = df[fill_mon_chung_cols].replace("", None).ffill()
        df["So_TC"] = 0
        df["Ten_lop"] = ""
    else:
        raise ValueError(f"subject_type khong hop le: {subject_type}")

    # Bo cac dong tiêu đề lặp lại ở mỗi trang ("Thứ", None...)
    df = df[df["Thu"].notna()]
    df = df[df["Thu"].astype(str).str.strip() != "Thứ"]
    df = df.dropna(subset=["TietBD", "So_tiet"])

    # Phan loai LT / TH dua tren o To_TH vua tach duoc
    loai_results = df.apply(determine_loai_and_to_th, axis=1)
    df["To_TH"] = [res[0] for res in loai_results]
    df["Loai"] = [res[1] for res in loai_results]

    return df[keep_cols]


def load_all_pdfs(pdf_paths: List[str], subject_type: str) -> pd.DataFrame:
    all_dfs = []
    for pdf_file in pdf_paths:
        df = load_pdf(pdf_file, subject_type)
        if not df.empty:
            all_dfs.append(df)
    if not all_dfs:
        return pd.DataFrame(columns=keep_cols)
    return pd.concat(all_dfs, ignore_index=True)


def normalize_text_columns(df: pd.DataFrame) -> pd.DataFrame:
    """
    Lam sach cac cot van ban:
      - LUU Y QUAN TRONG: Voi cot 'Tuan_hoc', TUYET DOI KHONG goi .strip() o dau chuoi
        de giu nguyen khoang trang bieu thi tuan nghi (vd '  *' = hoc tuan 3).
      - Voi cot 'Ma_phong', xoa khoang trang bi tach loi (vd '4.S_QP0 6' -> '4.S_QP06').
    """
    for col in df.select_dtypes(include=["object"]).columns:
        if col == "Tuan_hoc":
            df[col] = df[col].map(
                lambda v: v.replace("\r", "").replace("\n", "").rstrip(" ")
                if isinstance(v, str)
                else ""
            )
        elif col == "Ma_phong":
            df[col] = df[col].map(
                lambda v: re.sub(r"\s+", "", v.replace("\r", "").replace("\n", "")).rstrip(".")
                if isinstance(v, str)
                else ""
            )
        else:
            df[col] = df[col].map(
                lambda v: v.replace("\r", " ").replace("\n", " ").strip()
                if isinstance(v, str)
                else ""
            )
    return df


# ==============================================================================
# 4. TINH TUAN_MASK & NAP TRUC TIEP VAO MYSQL DOCKER
# ==============================================================================
def compute_tuan_mask(tuan_hoc: str, default_weeks: int = 15) -> Tuple[str, int]:
    """
    Chuyen chuoi Tuan_hoc sang (chuoi_tuan, tuan_mask 62-bit):
      - '***' -> hoc tuan 1, 2, 3 (mask = 0b111 = 7)
      - '  *' -> nghi tuan 1, 2; chi hoc tuan 3 (mask = 0b100 = 4)
    """
    raw = (tuan_hoc or "").replace("\r", "").replace("\n", "").rstrip(" ")
    if len(raw) == 0:
        return "*" * default_weeks, (1 << default_weeks) - 1

    mask = 0
    for idx, ch in enumerate(raw[:62]):
        if ch not in (" ", "-", "_", "."):
            mask |= 1 << idx

    if mask == 0:
        return "*" * default_weeks, (1 << default_weeks) - 1

    return raw[:62], mask


def import_dataframe_to_mysql(df: pd.DataFrame, ma_hoc_ky: str) -> None:
    """Nap DataFrame da xu ly vao 4 bang MySQL (hoc_ky, hoc_phan, nhom_lop, lich_hoc)."""
    if mysql is None:
        print("[THONG BAO] Chua cai mysql-connector-python (pip install mysql-connector-python) -> Bo qua buoc nap MySQL.")
        return

    db_config = {
        "host": os.environ.get("DB_HOST", "127.0.0.1"),
        "port": int(os.environ.get("DB_PORT", "3306")),
        "database": os.environ.get("DB_NAME", "tkb_db"),
        "user": os.environ.get("DB_USER", "root"),
        "password": os.environ.get("DB_PASS", "root"),
        "charset": "utf8mb4",
        "use_unicode": True,
    }

    parts = ma_hoc_ky.split("-")
    ten_hoc_ky = (
        f"Học kỳ {parts[2]} - Năm học {parts[0]}-{parts[1]}"
        if len(parts) == 3
        else f"Học kỳ {ma_hoc_ky}"
    )

    conn = mysql.connector.connect(**db_config)
    conn.autocommit = False
    cursor = conn.cursor()

    try:
        # Dam bao bang lich_hoc co du cot to_th, loai, tuan_hoc, tuan_mask va ck_lich_tiet <= 12
        cursor.execute("SHOW COLUMNS FROM lich_hoc")
        existing_cols = {row[0] for row in cursor.fetchall()}
        if "to_th" not in existing_cols:
            cursor.execute("ALTER TABLE lich_hoc ADD COLUMN to_th VARCHAR(5) NOT NULL DEFAULT '' AFTER phong")
        if "loai" not in existing_cols:
            cursor.execute("ALTER TABLE lich_hoc ADD COLUMN loai ENUM('LT','TH') NOT NULL DEFAULT 'LT' AFTER to_th")
        if "tuan_hoc" not in existing_cols:
            cursor.execute("ALTER TABLE lich_hoc ADD COLUMN tuan_hoc VARCHAR(30) NULL AFTER loai")
        if "tuan_mask" not in existing_cols:
            cursor.execute("ALTER TABLE lich_hoc ADD COLUMN tuan_mask BIGINT NOT NULL DEFAULT 32767 AFTER tuan_hoc")
        try:
            cursor.execute("ALTER TABLE lich_hoc DROP CHECK ck_lich_tiet")
            cursor.execute(
                "ALTER TABLE lich_hoc ADD CONSTRAINT ck_lich_tiet "
                "CHECK (tiet_bd >= 1 AND so_tiet >= 1 AND tiet_bd + so_tiet - 1 <= 12)"
            )
        except Exception:
            pass

        # 1. Upsert hoc_ky
        cursor.execute("UPDATE hoc_ky SET dang_dung = FALSE WHERE dang_dung = TRUE")
        cursor.execute(
            """
            INSERT INTO hoc_ky (ma, ten, dang_dung)
            VALUES (%s, %s, TRUE)
            ON DUPLICATE KEY UPDATE ten = VALUES(ten), dang_dung = TRUE
            """,
            (ma_hoc_ky, ten_hoc_ky),
        )

        # 2. Gom nhom hoc_phan va nhom_lop
        courses: Dict[str, Tuple[str, int]] = {}
        sections: Dict[Tuple[str, str], dict] = {}

        for row in df.itertuples(index=False):
            ma_mh = str(row.Ma_MH).strip()
            ten_mon = str(row.Ten_mon).strip()
            so_tc = int(row.So_TC) if str(row.So_TC).isdigit() else 0
            nhom = str(row.Nhom).strip().zfill(2)
            gv = str(row.Ho_ten_GV).strip()

            courses[ma_mh] = (ten_mon, max(so_tc, courses.get(ma_mh, (ten_mon, 0))[1]))

            sec = sections.setdefault(
                (ma_mh, nhom),
                {"ten_mh": ten_mon, "gv_list": [], "slots": [], "seen": set()},
            )
            if gv and gv.lower() != "họ và tên" and gv not in sec["gv_list"]:
                sec["gv_list"].append(gv)

            thu = int(row.Thu)
            tiet_bd = int(row.TietBD)
            so_tiet = int(row.So_tiet)
            if thu == 0 or tiet_bd == 0:
                continue
            if not (2 <= thu <= 8 and tiet_bd >= 1 and so_tiet >= 1 and tiet_bd + so_tiet - 1 <= 12):
                continue

            phong = str(row.Ma_phong).strip() or None
            to_th = str(row.To_TH).strip()
            loai = str(row.Loai).strip() or ("TH" if to_th else "LT")
            tuan_str, tuan_mask = compute_tuan_mask(str(row.Tuan_hoc))

            slot_key = (thu, tiet_bd, so_tiet, phong, to_th, loai, tuan_str, tuan_mask)
            if slot_key not in sec["seen"]:
                sec["seen"].add(slot_key)
                sec["slots"].append(slot_key)

        # 3. Ghi hoc_phan
        cursor.executemany(
            """
            INSERT INTO hoc_phan (ma_mh, ten, so_tc)
            VALUES (%s, %s, %s)
            ON DUPLICATE KEY UPDATE
                ten = VALUES(ten),
                so_tc = IF(VALUES(so_tc) > 0, VALUES(so_tc), so_tc)
            """,
            [(m, t, tc) for m, (t, tc) in courses.items()],
        )

        # 4. Ghi nhom_lop va lich_hoc
        total_slots = 0
        for (ma_mh, nhom), sec in sections.items():
            gv_merged = ", ".join(sec["gv_list"])[:100] if sec["gv_list"] else None
            cursor.execute(
                """
                INSERT INTO nhom_lop (ma_hoc_ky, ma_mh, ten_mh, nhom, giang_vien, dang_hoat_dong)
                VALUES (%s, %s, %s, %s, %s, TRUE)
                ON DUPLICATE KEY UPDATE
                    ten_mh = VALUES(ten_mh),
                    giang_vien = VALUES(giang_vien),
                    dang_hoat_dong = TRUE
                """,
                (ma_hoc_ky, ma_mh, sec["ten_mh"], nhom, gv_merged),
            )
            cursor.execute(
                "SELECT id FROM nhom_lop WHERE ma_hoc_ky = %s AND ma_mh = %s AND nhom = %s",
                (ma_hoc_ky, ma_mh, nhom),
            )
            nhom_lop_id = cursor.fetchone()[0]

            cursor.execute("DELETE FROM lich_hoc WHERE nhom_lop_id = %s", (nhom_lop_id,))
            if sec["slots"]:
                cursor.executemany(
                    """
                    INSERT INTO lich_hoc
                        (nhom_lop_id, thu, tiet_bd, so_tiet, phong, to_th, loai, tuan_hoc, tuan_mask)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                    """,
                    [(nhom_lop_id, *s) for s in sec["slots"]],
                )
                total_slots += len(sec["slots"])

        conn.commit()
        print(
            f"[MYSQL] Da nap thanh cong hoc ky {ma_hoc_ky}: "
            f"{len(courses)} hoc phan, {len(sections)} nhom lop, {total_slots} buoi hoc."
        )
    except Exception as e:
        conn.rollback()
        raise e
    finally:
        cursor.close()
        conn.close()


# ==============================================================================
# 5. CHUONG TRINH CHINH
# ==============================================================================
if __name__ == "__main__":
    print(f"=== BAT DAU BOC TACH PDF HOC KY {semester} ===")
    df_chuyen_nganh = load_all_pdfs(chuyen_nganh_paths, "chuyen_nganh")
    df_mon_chung = load_all_pdfs(mon_chung_paths, "mon_chung")

    df = pd.concat([df_chuyen_nganh, df_mon_chung], ignore_index=True)
    if df.empty:
        print("[CANH BAO] Khong doc duoc dong nao tu thu muc PDF! Kiem tra lai duong dan:", pdf_dir)
        sys.exit(1)

    df["So_TC"] = pd.to_numeric(df["So_TC"], errors="coerce").fillna(0).astype(int)
    df["Thu"] = pd.to_numeric(df["Thu"], errors="coerce").fillna(0).astype(int)
    df["TietBD"] = pd.to_numeric(df["TietBD"], errors="coerce").fillna(0).astype(int)
    df["So_tiet"] = pd.to_numeric(df["So_tiet"], errors="coerce").fillna(0).astype(int)

    df = normalize_text_columns(df)
    df = df.drop_duplicates( ignore_index=True)

    # Thong ke nhanh so luong buoi TH / LT tim thay
    th_count = (df["Loai"] == "TH").sum()
    lt_count = (df["Loai"] == "LT").sum()
    print(f"[THONG KE] Tong so dong sau khu trung: {len(df)} (LT: {lt_count}, TH co dau X/To_TH: {th_count})")

    os.makedirs(os.path.dirname(output_csv_path), exist_ok=True)
    df.to_csv(output_csv_path, index=False, encoding="utf-8")
    print(f"[CSV] Da luu file CSV tai: {output_csv_path}")

    # Neu muon nap thang vao MySQL Docker ngay sau khi xuat CSV (bo qua neu truyen --csv-only)
    if "--csv-only" not in sys.argv:
        import_dataframe_to_mysql(df, semester)