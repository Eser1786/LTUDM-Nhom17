#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Cong cu xu ly file CSV Thoi Khoa Bieu (co day du To_TH, Loai LT/TH, Tuan_hoc)
va nap truc tiep vao MySQL chay trong Docker.

Vi tri dat file:
    tools/import_tkb_mysql.py

Cai dat thu vien:
    pip install mysql-connector-python
"""

import csv
import os
import re
import sys
from pathlib import Path
from typing import Dict, List, Optional, Set, Tuple

try:
    import mysql.connector
except ImportError:
    mysql = None


def load_env_file(env_path: Path) -> None:
    """Nap bien moi truong tu file .env neu chua export trong shell."""
    if not env_path.exists():
        return
    for raw_line in env_path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        os.environ.setdefault(key, value)


PROJECT_ROOT = Path(__file__).resolve().parents[1]
load_env_file(PROJECT_ROOT / ".env")

# ==============================================================================
# 1. CAU HINH KET NOI MYSQL (DOCKER)
# ==============================================================================
def _first_env(*keys: str, default: str) -> str:
    for key in keys:
        value = os.environ.get(key)
        if value is not None and str(value).strip() != "":
            return value
    return default


def _env_int(*keys: str, default: int) -> int:
    raw = _first_env(*keys, default=str(default))
    try:
        return int(raw)
    except ValueError:
        return default


def build_db_config() -> Dict[str, object]:
    """
    Doc cau hinh ket noi tu bien moi truong theo thu tu uu tien:
    - Nhom MYSQL_* (phu hop docker-compose)
    - Nhom DB_* (tuong thich script cu)
    """
    return {
        "host": _first_env("MYSQL_HOST", "DB_HOST", default="127.0.0.1"),
        "port": _env_int("MYSQL_PORT", "DB_PORT", default=3306),
        "database": _first_env("MYSQL_DATABASE", "MYSQL_NAME", "DB_NAME", default="timetable_db"),
        "user": _first_env("MYSQL_USER", "DB_USER", default="timetable_user"),
        "password": _first_env("MYSQL_PASSWORD", "DB_PASS", default="timetable_password"),
        "charset": "utf8mb4",
        "use_unicode": True,
        "connection_timeout": 10,
    }


# ==============================================================================
# 2. HAM XU LY TUAN HOC (BITMASK) & PHAN LOAI LY THUYET / THUC HANH
# ==============================================================================
def compute_tuan_mask(tuan_hoc: str, default_weeks: int = 15) -> Tuple[str, int]:
    """
    Chuyen chuoi Tuan_hoc tu PDF/CSV sang (chuoi_nguyen_ban, tuan_mask 62-bit).
    QUAN TRONG:
      - KHONG dung .strip() hay .lstrip() xoa khoang trang dau chuoi, vi khoang trang ' '
        bieu thi tuan nghi!
      - Vi du:
          "***" -> hoc tuan 1, 2, 3 (bit 0, 1, 2 -> mask = 7)
          "  *" -> 2 khoang trang dau = nghi tuan 1, 2; chi hoc tuan 3 (bit 2 -> mask = 4)
          " * *" -> hoc tuan 2 va tuan 4 (bit 1, 3 -> mask = 10)
    Quy uoc bitmask trong 01-schema.sql:
      - Bit 0 (1 << 0) = Tuan 1
      - Bit 1 (1 << 1) = Tuan 2
      - Bit i (1 << i) = Tuan (i + 1)
    """
    if tuan_hoc is None:
        raw = ""
    else:
        # Chi xoa ky tu xuong dong (\r, \n) va khoang trang thua o CUOI chuoi,
        # GIU NGUYEN khoang trang o DAU va GIUA chuoi!
        raw = tuan_hoc.rstrip("\r\n ")

    # Neu chuoi hoan toan rong (file CSV cu chua co cot Tuan_hoc) -> mac dinh 15 tuan dau
    if len(raw) == 0:
        default_str = "*" * default_weeks
        return default_str, (1 << default_weeks) - 1

    # Ho tro them dinh dang khoang "1-15" hoac "3..10" neu nguoi dung nhap tay
    m_range = re.match(r"^\s*(\d{1,2})\s*[-..]+\s*(\d{1,2})\s*$", raw)
    if m_range:
        start_w = max(1, int(m_range.group(1)))
        end_w = min(62, int(m_range.group(2)))
        mask = 0
        chars = []
        for w in range(1, end_w + 1):
            if w >= start_w:
                mask |= 1 << (w - 1)
                chars.append("*")
            else:
                chars.append(" ")
        return "".join(chars), mask

    # Duyet tung vi tri ky tu (tuan 1 = index 0, tuan 2 = index 1, ...):
    # Khoang trang ' ' (hoac '-', '_') la tuan NGHI (bit = 0)
    # Dau '*' (hoac so/chu bat ky) la tuan CO HOC (bit = 1)
    mask = 0
    for idx, ch in enumerate(raw[:62]):
        if ch not in (" ", "-", "_", "."):
            mask |= 1 << idx

    # Dam bao tuan_mask > 0 theo rang buoc ck_lich_tuan
    if mask == 0:
        default_str = "*" * default_weeks
        return default_str, (1 << default_weeks) - 1

    return raw[:62], mask


def classify_lt_th(
    ten_mon: str,
    to_th_raw: str,
    loai_raw: str,
    so_tiet: int,
    ma_phong: str,
    slots_in_group: List[dict],
) -> Tuple[str, str]:
    """
    Xac dinh (to_th, loai) voi loai in ('LT', 'TH').
    - Neu PDF/CSV da co san To_TH (vd '01', '02') hoac Loai ('TH'/'LT') -> giu nguyen.
    - Neu PDF khong danh dau o Thuc hanh (nhu ghi chu A5 cua nhom), tu dong suy luan:
        + Ten mon bat dau bang 'Thực hành', 'Thí nghiệm', 'Thực tập', 'Thực tế' -> 'TH'
        + Hoac trong cung 1 nhom lop vua co buoi 2-3 tiet va buoi 5 tiet (phong lab/may tinh)
          thi buoi 5 tiet la 'TH', buoi 2-3 tiet la 'LT'.
    """
    to_th = (to_th_raw or "").strip()
    loai = (loai_raw or "").strip().upper()

    if loai in ("LT", "TH"):
        if loai == "TH" and not to_th:
            to_th = "01"
        elif loai == "LT":
            to_th = ""
        return to_th, loai

    if to_th:
        return to_th.zfill(2), "TH"

    ten_lower = ten_mon.lower()
    if any(
        kw in ten_lower
        for kw in ("thực hành", "thí nghiệm", "thực tập", "thực tế", "ký họa thực tế")
    ):
        return "01", "TH"

    # Neu nhom lop co ca buoi < 5 tiet va buoi == 5 tiet -> buoi 5 tiet thuong la TH phong may/lab
    has_short_slot = any(0 < s["so_tiet"] < 5 for s in slots_in_group)
    if so_tiet >= 5 and has_short_slot:
        return "01", "TH"

    return "", "LT"


def clean_room(room_raw: str) -> Optional[str]:
    """Xoa khoang trang bi loi khi tach PDF (vd '4.S_QP0 6' -> '4.S_QP06') va dau cham cuoi."""
    if not room_raw:
        return None
    cleaned = re.sub(r"\s+", "", room_raw.strip()).rstrip(".")
    return cleaned if cleaned else None


# ==============================================================================
# 3. DOC CSV, BO SUNG COT (TO_TH, LOAI, TUAN_HOC) & GHI LAI FILE CSV CHUAN
# ==============================================================================
def process_and_enrich_csv(
    input_csv_path: Path, output_csv_path: Optional[Path] = None
) -> Tuple[Dict[str, Tuple[str, int]], Dict[Tuple[str, str], dict]]:
    """
    Doc file CSV goc (8 cot hoac 12 cot), tu dong dien cac cot con thieu
    (So_TC, To_TH, Loai, Tuan_hoc), khu trung lap va ghi de/xuat file CSV day du.
    """
    if not input_csv_path.exists():
        raise FileNotFoundError(f"Khong tim thay file CSV: {input_csv_path}")

    with open(input_csv_path, mode="r", encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        raw_rows = list(reader)

    # Gom nhom tam de suy luan LT/TH chinh xac theo tung (Ma_MH, Nhom)
    grouped_raw: Dict[Tuple[str, str], List[dict]] = {}
    for r in raw_rows:
        ma_mh = (r.get("Ma_MH") or "").strip()
        nhom = (r.get("Nhom") or "").strip().zfill(2)
        if not ma_mh or not nhom:
            continue
        r["_ma_mh"] = ma_mh
        r["_nhom"] = nhom
        r["_thu"] = int(r.get("Thu") or 0)
        r["_tiet_bd"] = int(r.get("TietBD") or 0)
        r["_so_tiet"] = int(r.get("So_tiet") or 0)
        grouped_raw.setdefault((ma_mh, nhom), []).append(r)

    courses: Dict[str, Tuple[str, int]] = {}  # ma_mh -> (ten_mon, so_tc)
    sections: Dict[Tuple[str, str], dict] = {}  # (ma_mh, nhom) -> info + slots
    enriched_csv_rows: List[dict] = []
    seen_csv_keys: Set[tuple] = set()

    skipped_zero_rows = 0
    duplicate_rows = 0

    for (ma_mh, nhom), group_rows in grouped_raw.items():
        # Thong tin danh muc cho nhom lop
        first = group_rows[0]
        ten_mon = (first.get("Ten_mon") or "").strip()
        so_tc = int(first.get("So_TC") or 0)
        courses.setdefault(ma_mh, (ten_mon, so_tc))

        sec = sections.setdefault(
            (ma_mh, nhom),
            {
                "ma_mh": ma_mh,
                "ten_mh": ten_mon,
                "nhom": nhom,
                "si_so": int(first["Si_so"]) if first.get("Si_so") else None,
                "giang_vien_list": [],
                "ma_vc": (first.get("Ma_VC") or "").strip() or None,
                "ten_lop": (first.get("Ten_lop") or "").strip() or None,
                "slots": [],
                "seen_slots": set(),
            },
        )

        # Chuan bi danh sach so_tiet trong nhom de phan loai LT/TH
        slots_meta = [{"so_tiet": r["_so_tiet"]} for r in group_rows]

        for r in group_rows:
            gv = (r.get("Ho_ten_GV") or "").strip()
            if gv and gv.lower() != "họ và tên" and gv not in sec["giang_vien_list"]:
                sec["giang_vien_list"].append(gv)

            thu = r["_thu"]
            tiet_bd = r["_tiet_bd"]
            so_tiet = r["_so_tiet"]
            phong = clean_room(r.get("Ma_phong") or "")

            to_th, loai = classify_lt_th(
                ten_mon=ten_mon,
                to_th_raw=r.get("To_TH") or "",
                loai_raw=r.get("Loai") or "",
                so_tiet=so_tiet,
                ma_phong=phong or "",
                slots_in_group=slots_meta,
            )
            # GIU NGUYEN khoang trang dau chuoi Tuan_hoc (vd "  *" = hoc tuan 3)
            tuan_str, tuan_mask = compute_tuan_mask(r.get("Tuan_hoc"))

            # Luu dong da bo sung day du cot de xuat ra CSV (khu trung lap dong CSV)
            csv_dedup_key = (ma_mh, nhom, to_th, loai, thu, tiet_bd, so_tiet, phong or "", tuan_str)
            if csv_dedup_key not in seen_csv_keys:
                seen_csv_keys.add(csv_dedup_key)
                enriched_csv_rows.append(
                    {
                        "Ma_MH": ma_mh,
                        "Ten_mon": ten_mon,
                        "So_TC": so_tc,
                        "Ho_ten_GV": gv,
                        "Nhom": nhom,
                        "To_TH": to_th,
                        "Loai": loai,
                        "Thu": thu,
                        "TietBD": tiet_bd,
                        "So_tiet": so_tiet,
                        "Ma_phong": phong or "",
                        "Tuan_hoc": tuan_str,
                    }
                )
            else:
                duplicate_rows += 1

            # Kiem tra dieu kien chen vao bang lich_hoc trong MySQL
            if thu == 0 or tiet_bd == 0:
                skipped_zero_rows += 1
                continue

            tiet_ket_thuc = tiet_bd + so_tiet - 1
            if not (2 <= thu <= 8 and tiet_bd >= 1 and so_tiet >= 1 and tiet_ket_thuc <= 12):
                print(
                    f"[CANH BAO] Bo qua lich ngoai pham vi: {ma_mh} nhom {nhom} "
                    f"(Thu={thu}, TietBD={tiet_bd}, SoTiet={so_tiet})"
                )
                continue

            slot_db_key = (thu, tiet_bd, so_tiet, phong, to_th, loai, tuan_str, tuan_mask)
            if slot_db_key not in sec["seen_slots"]:
                sec["seen_slots"].add(slot_db_key)
                sec["slots"].append(
                    {
                        "thu": thu,
                        "tiet_bd": tiet_bd,
                        "so_tiet": so_tiet,
                        "phong": phong,
                        "to_th": to_th,
                        "loai": loai,
                        "tuan_hoc": tuan_str,
                        "tuan_mask": tuan_mask,
                    }
                )

    # Ghi lai file CSV co day du cac cot To_TH, Loai, Tuan_hoc
    target_out = output_csv_path or input_csv_path
    fieldnames = [
        "Ma_MH",
        "Ten_mon",
        "So_TC",
        "Ho_ten_GV",
        "Nhom",
        "To_TH",
        "Loai",
        "Thu",
        "TietBD",
        "So_tiet",
        "Ma_phong",
        "Tuan_hoc",
    ]
    with open(target_out, mode="w", encoding="utf-8", newline="") as f_out:
        writer = csv.DictWriter(f_out, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(enriched_csv_rows)

    print(
        f"[CSV] Da xu ly {len(raw_rows)} dong goc -> Xuat {len(enriched_csv_rows)} dong chuan hoa vao '{target_out}' "
        f"(loai bo {duplicate_rows} dong trung, {skipped_zero_rows} buoi Thu=0)."
    )
    return courses, sections


# ==============================================================================
# 4. NAP DU LIEU VAO MYSQL TRONG 1 TRANSACTION
# ==============================================================================
def ensure_schema_columns_and_constraints(cursor) -> None:
    """
    Dam bao bang lich_hoc trong MySQL da mo cac cot to_th, loai, tuan_hoc, tuan_mask
    va cho phep tiet hoc den tiet 12 (xu ly 34 buoi tiet 9-11).
    """
    cursor.execute("SHOW COLUMNS FROM lich_hoc")
    rows = cursor.fetchall()
    existing_cols = {row[0] for row in rows}

    if "to_th" not in existing_cols:
        cursor.execute(
            "ALTER TABLE lich_hoc ADD COLUMN to_th VARCHAR(5) NOT NULL DEFAULT '' AFTER phong"
        )
    if "loai" not in existing_cols:
        cursor.execute(
            "ALTER TABLE lich_hoc ADD COLUMN loai ENUM('LT','TH') NOT NULL DEFAULT 'LT' AFTER to_th"
        )
    if "tuan_hoc" not in existing_cols:
        cursor.execute(
            "ALTER TABLE lich_hoc ADD COLUMN tuan_hoc VARCHAR(62) NULL AFTER loai"
        )
    else:
        # Tuong thich DB cu: neu tuan_hoc nho hon 62 ky tu thi mo rong.
        col_map = {row[0]: row for row in rows}
        tuan_hoc_row = col_map.get("tuan_hoc")
        if tuan_hoc_row is not None:
            col_type = str(tuan_hoc_row[1]).lower()  # vd: 'varchar(30)'
            m = re.search(r"varchar\((\d+)\)", col_type)
            if m and int(m.group(1)) < 62:
                cursor.execute("ALTER TABLE lich_hoc MODIFY COLUMN tuan_hoc VARCHAR(62) NULL")
    if "tuan_mask" not in existing_cols:
        cursor.execute(
            "ALTER TABLE lich_hoc ADD COLUMN tuan_mask BIGINT NOT NULL DEFAULT 32767 AFTER tuan_hoc"
        )

    # Cap nhat CHECK constraint ck_lich_tiet len <= 12
    try:
        cursor.execute("ALTER TABLE lich_hoc DROP CHECK ck_lich_tiet")
    except Exception:
        pass
    try:
        cursor.execute(
            "ALTER TABLE lich_hoc ADD CONSTRAINT ck_lich_tiet "
            "CHECK (tiet_bd >= 1 AND so_tiet >= 1 AND tiet_bd + so_tiet - 1 <= 12)"
        )
    except Exception:
        pass


def import_to_mysql(
    ma_hoc_ky: str,
    ten_hoc_ky: str,
    courses: Dict[str, Tuple[str, int]],
    sections: Dict[Tuple[str, str], dict],
) -> None:
    if mysql is None:
        raise RuntimeError(
            "Chua cai thu vien mysql-connector-python! Chay lenh: pip install mysql-connector-python"
        )

    db_config = build_db_config()
    try:
        conn = mysql.connector.connect(**db_config)
    except Exception as exc:
        safe_cfg = {k: v for k, v in db_config.items() if k != "password"}
        raise RuntimeError(
            "Khong ket noi duoc MySQL. Kiem tra docker compose, host/port va tai khoan trong .env. "
            f"Cau hinh dang dung: {safe_cfg}"
        ) from exc
    conn.autocommit = False
    cursor = conn.cursor()

    try:
        # 1. Kiem tra va nang cap schema bang lich_hoc neu can
        ensure_schema_columns_and_constraints(cursor)

        # 2. Upsert hoc_ky (dat hoc ky moi lam hoc ky dang_dung = TRUE)
        cursor.execute("UPDATE hoc_ky SET dang_dung = FALSE WHERE dang_dung = TRUE")
        cursor.execute(
            """
            INSERT INTO hoc_ky (ma, ten, dang_dung)
            VALUES (%s, %s, TRUE)
            ON DUPLICATE KEY UPDATE ten = VALUES(ten), dang_dung = TRUE
            """,
            (ma_hoc_ky, ten_hoc_ky),
        )

        # 3. Upsert hoc_phan
        course_params = [
            (ma_mh, ten_mon, so_tc) for ma_mh, (ten_mon, so_tc) in courses.items()
        ]
        cursor.executemany(
            """
            INSERT INTO hoc_phan (ma_mh, ten, so_tc)
            VALUES (%s, %s, %s)
            ON DUPLICATE KEY UPDATE
                ten = VALUES(ten),
                so_tc = IF(VALUES(so_tc) > 0, VALUES(so_tc), so_tc)
            """,
            course_params,
        )

        # 4. Upsert nhom_lop va lich_hoc
        total_slots = 0
        for (ma_mh, nhom), sec in sections.items():
            gv_merged = ", ".join(sec["giang_vien_list"])
            gv_merged = gv_merged[:100] if gv_merged else None

            cursor.execute(
                """
                INSERT INTO nhom_lop (ma_hoc_ky, ma_mh, ten_mh, nhom, si_so, giang_vien, ma_vc, ten_lop, dang_hoat_dong)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, TRUE)
                ON DUPLICATE KEY UPDATE
                    ten_mh = VALUES(ten_mh),
                    si_so = COALESCE(VALUES(si_so), si_so),
                    giang_vien = VALUES(giang_vien),
                    ma_vc = COALESCE(VALUES(ma_vc), ma_vc),
                    ten_lop = COALESCE(VALUES(ten_lop), ten_lop),
                    dang_hoat_dong = TRUE
                """,
                (
                    ma_hoc_ky,
                    ma_mh,
                    sec["ten_mh"],
                    nhom,
                    sec["si_so"],
                    gv_merged,
                    sec["ma_vc"],
                    sec["ten_lop"],
                ),
            )

            cursor.execute(
                "SELECT id FROM nhom_lop WHERE ma_hoc_ky = %s AND ma_mh = %s AND nhom = %s",
                (ma_hoc_ky, ma_mh, nhom),
            )
            nhom_lop_id = cursor.fetchone()[0]

            # Xoa lich_hoc cu cua nhom va chen lai
            cursor.execute("DELETE FROM lich_hoc WHERE nhom_lop_id = %s", (nhom_lop_id,))

            if sec["slots"]:
                slot_params = [
                    (
                        nhom_lop_id,
                        s["thu"],
                        s["tiet_bd"],
                        s["so_tiet"],
                        s["phong"],
                        s["to_th"],
                        s["loai"],
                        s["tuan_hoc"],
                        s["tuan_mask"],
                    )
                    for s in sec["slots"]
                ]
                cursor.executemany(
                    """
                    INSERT INTO lich_hoc
                        (nhom_lop_id, thu, tiet_bd, so_tiet, phong, to_th, loai, tuan_hoc, tuan_mask)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                    """,
                    slot_params,
                )
                total_slots += len(slot_params)

        conn.commit()
        print(
            f"[MYSQL] Da nap thanh cong vao hoc ky {ma_hoc_ky}: "
            f"{len(courses)} hoc phan, {len(sections)} nhom lop, {total_slots} buoi hoc."
        )
    except Exception as exc:
        conn.rollback()
        raise exc
    finally:
        cursor.close()
        conn.close()


def infer_semester_from_path(csv_path: Path) -> Tuple[str, str]:
    parent_name = csv_path.parent.name
    if re.match(r"^\d{4}-\d{4}-\d$", parent_name):
        parts = parent_name.split("-")
        return parent_name, f"Học kỳ {parts[2]} - Năm học {parts[0]}-{parts[1]}"
    return "2026-2027-1", "Học kỳ 1 - Năm học 2026-2027"


def main():
    csv_arg = sys.argv[1] if len(sys.argv) > 1 else "csv/2026-2027-1/thoi_khoa_bieu.csv"
    csv_path = Path(csv_arg)
    ma_hk, ten_hk = infer_semester_from_path(csv_path)

    print(f"=== XU LY CSV & NAP MYSQL CHO HOC KY: {ma_hk} ===")
    courses, sections = process_and_enrich_csv(csv_path)

    if "--csv-only" in sys.argv:
        print("Da cap nhat xong file CSV (--csv-only, bo qua buoc ket noi MySQL).")
        return

    import_to_mysql(ma_hk, ten_hk, courses, sections)


if __name__ == "__main__":
    main()