#!/usr/bin/env python3
"""Download PDF thời khóa biểu theo học kỳ được cấu hình linh hoạt."""

import argparse
import os
from pathlib import Path
from typing import Iterable, List, Sequence

import requests as rq

DEFAULT_SEMESTER = "2026-2027-1"
DEFAULT_BASE_URL = "https://daotao.sgu.edu.vn/images/pdf/sotaydkmh/261/"
DEFAULT_COURSES = [
    "TKB_monchung", "DKD", "DCM", "DCV", "DCT", "DDL", "DGD", "DGM", "DGT", "DKE", "DDU",
    "DKM", "DTT", "DKQ", "DKI", "DDE", "DDV", "DKP", "DLU", "DAN", "DQG", "DQK", "DHA",
    "DQV", "DQT", "DNH", "DDI", "DHO", "DKH", "DSU", "DLD", "DMI", "DVA", "DSI", "DSA",
    "DTO", "DLI", "DTN", "DTL", "DTU", "DNT", "DVI", "DLS", "DDH",
]


def resolve_semester(explicit: str | None = None) -> str:
    value = explicit.strip() if explicit and explicit.strip() else os.getenv("SEMESTER", DEFAULT_SEMESTER)
    return value.strip() or DEFAULT_SEMESTER


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Tải PDF thời khóa biểu cho một học kỳ cụ thể.")
    parser.add_argument("--semester", default=None, help="Mã học kỳ, ví dụ 2026-2027-1")
    parser.add_argument("--base-url", default=os.getenv("PDF_BASE_URL", DEFAULT_BASE_URL), help="Base URL nơi chứa PDF.")
    parser.add_argument("--output-dir", default=None, help="Thư mục lưu PDF. Mặc định: pdf/<semester>")
    parser.add_argument("--course", action="append", dest="courses", default=None, help="Tải 1 môn cụ thể. Có thể lặp lại flag này.")
    return parser.parse_args()


def download_pdf(course: str, semester: str, base_url: str = DEFAULT_BASE_URL, output_dir: str | Path | None = None) -> Path | None:
    course_code = course.strip()
    if not course_code:
        return None

    target_dir = Path(output_dir) if output_dir else Path("pdf") / semester
    target_dir.mkdir(parents=True, exist_ok=True)
    target_path = target_dir / f"{course_code}.pdf"

    try:
        response = rq.get(f"{base_url.rstrip('/')}/{course_code}.pdf", timeout=30)
        if response.status_code != 200:
            print(f"[FAILED] {course_code}.pdf => HTTP {response.status_code}")
            return None
        target_path.write_bytes(response.content)
        print(f"[OK] {course_code}.pdf -> {target_path}")
        return target_path
    except Exception as exc:  # pragma: no cover - network issues fallback
        print(f"[ERROR] Không tải được {course_code}.pdf: {exc}")
        return None


def download_semester_pdfs(
    semester: str,
    base_url: str = DEFAULT_BASE_URL,
    output_dir: str | Path | None = None,
    course_codes: Sequence[str] | None = None,
) -> List[Path]:
    selected_courses = list(course_codes) if course_codes else DEFAULT_COURSES
    target_dir = Path(output_dir) if output_dir else Path("pdf") / semester
    target_dir.mkdir(parents=True, exist_ok=True)

    downloaded: List[Path] = []
    for course in selected_courses:
        result = download_pdf(course, semester, base_url=base_url, output_dir=target_dir)
        if result is not None:
            downloaded.append(result)
    return downloaded


def main() -> None:
    args = parse_args()
    semester = resolve_semester(args.semester)
    courses = args.courses if args.courses else DEFAULT_COURSES
    download_semester_pdfs(
        semester=semester,
        base_url=args.base_url,
        output_dir=args.output_dir,
        course_codes=courses,
    )


if __name__ == "__main__":
    main()