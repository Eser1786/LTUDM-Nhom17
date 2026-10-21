import pdfplumber
import pandas as pd
import json


courses = [ "DKD", "DCM", "DCV", "DCT", "DDL", "DGD", "DGM", "DGT", "DKE", "DDU", "DKM", "DTT", "DKQ", "DKI", "DDE", "DDV", "DKP", "DLU", "DAN", "DQG", "DQK", "DHA", "DQV", "DQT", "DNH", "DDI", "DHO", "DKH", "DSU", "DLD", "DMI", "DVA", "DSI", "DSA", "DTO", "DLI", "DTN", "DTL", "DTU", "DNT", "DVI", "DLS", "DDH"]

semester = "2026-2027-1"

# "TKB_monchung"

pdf_path = "pdf/2026-2027-1/"

"""những danh sách này là của chuyên ngành"""
chuyen_nganh_full = ["STT", "Ma_MH", "Ten_mon","So_TC","Si_so","Ho_ten_GV",
                "Ma_VC","Nhom","To_TH","Thu","TietBD","So_tiet",
                "Ma_phong","Ten_lop","Tuan_hoc" ] 

fill_chuyen_nganh_cols = ["Ma_MH","Ten_mon","So_TC","Ho_ten_GV","Nhom"]

keep_chuyen_nganh_cols = ["Ma_MH","Ten_mon","Ho_ten_GV","Nhom",
             "Thu","TietBD","So_tiet","Ma_phong"]


"""Những danh sách này là của môn chung"""
mon_chung_full = ["Ma_VC", "Ho_ten_GV", "Ma_MH", "Nhom", "To_TH", "Ten_mon", "Si_so", "Thu", "TietBD", "So_tiet", "Ma_phong", "Tuan_hoc" ] 

fill_mon_chung_cols = ["Ma_MH","Ten_mon","Ho_ten_GV","Nhom"]

keep_mon_chung_cols = ["Ma_MH","Ten_mon","Ho_ten_GV","Nhom",
             "Thu","TietBD","So_tiet","Ma_phong"]


"""Mon chung co 12 rows, mon chuyen nganh co 15 rows"""
# pdf_paths = ["data/raw/DCT.pdf", "data/raw/MonChung.pdf"]

chuyen_nganh =[]

for course in courses:
    chuyen_nganh.append(pdf_path + course + ".pdf")

mon_chung = [pdf_path + "TKB_monchung.pdf"]


"""chuyen nganh co 15 rows"""
# chuyen_nganh = ["backend/data/raw/DCT.pdf"]

"""mon chung co 12 rows"""
# mon_chung = ["backend/data/raw/MonChung.pdf"]


def extract_rows(pdf_path):
    all_rows = []
    with pdfplumber.open(pdf_path) as pdf:
        for page in pdf.pages:
            table = page.extract_table()
            if table:
                all_rows.extend(table)
    return all_rows

# print(len(extract_rows("backend/data/DCT.pdf")))

"""Đọc 1 pdf và trả về df"""
def load_pdf(pdf_path, subject_type):
    rows = extract_rows(pdf_path)
    if subject_type == "chuyen_nganh":
        df = pd.DataFrame(rows, columns=chuyen_nganh_full)
        df[fill_chuyen_nganh_cols] = df[fill_chuyen_nganh_cols].replace("", None).ffill()
        df = df[keep_chuyen_nganh_cols]
    elif subject_type == "mon_chung":
        df = pd.DataFrame(rows, columns=mon_chung_full)
        df[fill_mon_chung_cols] = df[fill_mon_chung_cols].replace("", None).ffill()
        df = df[keep_mon_chung_cols]
    return df

"""Đọc tất cả các file pdf và trả về dataframe chứa dữ liệu từ tất cả các file"""
def load_all_pdfs(pdf_paths, subject_type):
    all_dfs = []
    for pdf_file in pdf_paths:
        df = load_pdf(pdf_file, subject_type)
        all_dfs.append(df)
    return pd.concat(all_dfs, ignore_index=True)


def normalize_text_columns(df):
    for col in df.select_dtypes(include=["object"]).columns:
        df[col] = df[col].map(lambda v: v.replace("\r", " ").replace("\n", " ").strip() if isinstance(v, str) else v)
    return df


chuyen_nganh = load_all_pdfs(chuyen_nganh, "chuyen_nganh")


chuyen_nganh = chuyen_nganh[chuyen_nganh["Thu"].notna()]
chuyen_nganh = chuyen_nganh[chuyen_nganh["Thu"] != "Thứ"]
chuyen_nganh = chuyen_nganh.dropna(subset=["TietBD", "So_tiet"])

chuyen_nganh["Thu"] = chuyen_nganh["Thu"].astype(int)
chuyen_nganh["TietBD"] = chuyen_nganh["TietBD"].astype(int)
chuyen_nganh["So_tiet"] = chuyen_nganh["So_tiet"].astype(int)

# chuyen_nganh.to_csv("csv/2026-2027-1/chuyen_nganh.csv", index=False)

mon_chung = load_all_pdfs(mon_chung, "mon_chung")


mon_chung = mon_chung[mon_chung["Thu"].notna()]
mon_chung = mon_chung[mon_chung["Thu"] != "Thứ"]
mon_chung = mon_chung.dropna(subset=["TietBD", "So_tiet"])

mon_chung["Thu"] = mon_chung["Thu"].astype(int)
mon_chung["TietBD"] = mon_chung["TietBD"].astype(int)
mon_chung["So_tiet"] = mon_chung["So_tiet"].astype(int)

# mon_chung.to_csv("csv/2026-2027-1/mon_chung.csv", index=False)

df = pd.concat([chuyen_nganh, mon_chung], ignore_index=True)
df = normalize_text_columns(df)

df.to_csv("csv/2026-2027-1/thoi_khoa_bieu.csv", index=False)


# def build_sections(df):
#     df["section_id"] = df["Ma_MH"] + "_" + df["Nhom"]
#     section = {}

#     for sid, group in df.groupby("section_id"):
#         sessions = []
#         for row in group.itertuples(index=False):
#             sessions.append({
#                 "Thu": row.Thu,
#                 "TietBD": row.TietBD,
#                 "So_tiet": row.So_tiet,
#                 "Ma_phong": row.Ma_phong
#             })
        
#         section[sid] = {
#             "Ma_MH": row.Ma_MH,
#             "Ten_mon": row.Ten_mon,
#             "Ho_ten_GV": row.Ho_ten_GV,
#             "Nhom": row.Nhom,
#             "sessions": sessions
#         }
            
#     return section

# sections = build_sections(df)

# print(json.dumps(sections["841022_01"], indent=4, ensure_ascii=False))

# with open ("backend/data/json/sections.json", "w", encoding="utf-8") as f:
#     json.dump(sections, f, indent=2, ensure_ascii=False)