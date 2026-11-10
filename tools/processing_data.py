#!/usr/bin/env python3
"""Legacy pipeline: xử lý PDF sang CSV theo học kỳ cấu hình động."""

import argparse
import os
from pathlib import Path

import pandas as pd
import pdfplumber

DEFAULT_SEMESTER = "2026-2027-1"
COURSES = [
    "DKD", "DCM", "DCV", "DCT", "DDL", "DGD", "DGM", "DGT", "DKE", "DDU", "DKM", "DTT",
    "DKQ", "DKI", "DDE", "DDV", "DKP", "DLU", "DAN", "DQG", "DQK", "DHA", "DQV", "DQT",
    "DNH", "DDI", "DHO", "DKH", "DSU", "DLD", "DMI", "DVA", "DSI", "DSA", "DTO", "DLI",
    "DTN", "DTL", "DTU", "DNT", "DVI", "DLS", "DDH",
]


def resolve_semester(explicit: str | None = None) -> str:
    value = explicit.strip() if explicit and explicit.strip() else os.getenv("SEMESTER", DEFAULT_SEMESTER)
    return value.strip() or DEFAULT_SEMESTER


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Chuyển PDF thời khóa biểu thành CSV theo học kỳ.")
    parser.add_argument("--semester", default=None, help="Mã học kỳ, ví dụ 2026-2027-1")
    parser.add_argument("--pdf-dir", default=None, help="Thư mục chứa PDF. Mặc định: pdf/<semester>")
    parser.add_argument("--csv-dir", default=None, help="Thư mục lưu CSV. Mặc định: csv/<semester>")
    return parser.parse_args()


args = parse_args()
semester = resolve_semester(args.semester)
pdf_path = Path(args.pdf_dir) if args.pdf_dir else Path("pdf") / semester
csv_dir = Path(args.csv_dir) if args.csv_dir else Path("csv") / semester
output_csv_path = csv_dir / "thoi_khoa_bieu.csv"

courses = COURSES

"""những danh sách này là của chuyên ngành"""
chuyen_nganh_full = [
    "STT", "Ma_MH", "Ten_mon", "So_TC", "Si_so", "Ho_ten_GV",
    "Ma_VC", "Nhom", "To_TH", "Thu", "TietBD", "So_tiet",
    "Ma_phong", "Ten_lop", "Tuan_hoc",
]

fill_chuyen_nganh_cols = ["Ma_MH", "Ten_mon", "So_TC", "Ho_ten_GV", "Nhom"]
keep_chuyen_nganh_cols = ["Ma_MH", "Ten_mon", "So_TC", "Ho_ten_GV", "Nhom", "Thu", "TietBD", "So_tiet", "Ma_phong"]

"""Những danh sách này là của môn chung"""
mon_chung_full = ["Ma_VC", "Ho_ten_GV", "Ma_MH", "Nhom", "To_TH", "Ten_mon", "Si_so", "Thu", "TietBD", "So_tiet", "Ma_phong", "Tuan_hoc"]
fill_mon_chung_cols = ["Ma_MH", "Ten_mon", "Ho_ten_GV", "Nhom"]
keep_mon_chung_cols = ["Ma_MH", "Ten_mon", "Ho_ten_GV", "Nhom", "Thu", "TietBD", "So_tiet", "Ma_phong"]

chuyen_nganh = []
for course in courses:
    p = str(pdf_path / f"{course}.pdf")
    if os.path.exists(p):
        chuyen_nganh.append(p)

mon_chung = [str(pdf_path / "TKB_monchung.pdf")] if os.path.exists(str(pdf_path / "TKB_monchung.pdf")) else []


def extract_rows(path_value):
    all_rows = []
    with pdfplumber.open(path_value) as pdf:
        for page in pdf.pages:
            table = page.extract_table()
            if table:
                all_rows.extend(table)
    return all_rows


def load_pdf(path_value, subject_type):
    rows = extract_rows(path_value)
    if subject_type == "chuyen_nganh":
        df = pd.DataFrame(rows, columns=chuyen_nganh_full)
        df[fill_chuyen_nganh_cols] = df[fill_chuyen_nganh_cols].replace("", None).ffill()
        df = df[keep_chuyen_nganh_cols]
    elif subject_type == "mon_chung":
        df = pd.DataFrame(rows, columns=mon_chung_full)
        df[fill_mon_chung_cols] = df[fill_mon_chung_cols].replace("", None).ffill()
        df = df[keep_mon_chung_cols]
    else:
        return pd.DataFrame()
    return df


def load_all_pdfs(pdf_paths, subject_type):
    all_dfs = []
    for pdf_file in pdf_paths:
        df = load_pdf(pdf_file, subject_type)
        all_dfs.append(df)
    if not all_dfs:
        cols = chuyen_nganh_full if subject_type == "chuyen_nganh" else mon_chung_full
        return pd.DataFrame(columns=cols)
    return pd.concat(all_dfs, ignore_index=True)


def normalize_text_columns(df):
    for col in df.select_dtypes(include=["object"]).columns:
        df[col] = df[col].map(lambda v: v.replace("\r", " ").replace("\n", " ").strip() if isinstance(v, str) else v)
    return df


chuyen_nganh_df = load_all_pdfs(chuyen_nganh, "chuyen_nganh")
chuyen_nganh_df = chuyen_nganh_df[chuyen_nganh_df["Thu"].notna()]
chuyen_nganh_df = chuyen_nganh_df[chuyen_nganh_df["Thu"] != "Thứ"]
chuyen_nganh_df = chuyen_nganh_df.dropna(subset=["TietBD", "So_tiet"])
chuyen_nganh_df["Thu"] = chuyen_nganh_df["Thu"].astype(int)
chuyen_nganh_df["TietBD"] = chuyen_nganh_df["TietBD"].astype(int)
chuyen_nganh_df["So_tiet"] = chuyen_nganh_df["So_tiet"].astype(int)

mon_chung_df = load_all_pdfs(mon_chung, "mon_chung")
mon_chung_df = mon_chung_df[mon_chung_df["Thu"].notna()]
mon_chung_df = mon_chung_df[mon_chung_df["Thu"] != "Thứ"]
mon_chung_df = mon_chung_df.dropna(subset=["TietBD", "So_tiet"])
mon_chung_df["Thu"] = mon_chung_df["Thu"].astype(int)
mon_chung_df["TietBD"] = mon_chung_df["TietBD"].astype(int)
mon_chung_df["So_tiet"] = mon_chung_df["So_tiet"].astype(int)

df = pd.concat([chuyen_nganh_df, mon_chung_df], ignore_index=True)
df = normalize_text_columns(df)

csv_dir.mkdir(parents=True, exist_ok=True)
df.to_csv(output_csv_path, index=False)
print(f"[CSV] Đã xuất {output_csv_path} cho học kỳ {semester}")