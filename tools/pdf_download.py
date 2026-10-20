import requests as rq
from pathlib import Path

url = "https://daotao.sgu.edu.vn/images/pdf/sotaydkmh/261/"

# https://daotao.sgu.edu.vn/images/pdf/sotaydkmh/261/TKB_monchung.pdf

courses = ["TKB_monchung", "DKD", "DCM", "DCV", "DCT", "DDL", "DGD", "DGM", "DGT", "DKE", "DDU", "DKM", "DTT", "DKQ", "DKI", "DDE", "DDV", "DKP", "DLU", "DAN", "DQG", "DQK", "DHA", "DQV", "DQT", "DNH", "DDI", "DHO", "DKH", "DSU", "DLD", "DMI", "DVA", "DSI", "DSA", "DTO", "DLI", "DTN", "DTL", "DTU", "DNT", "DVI", "DLS", "DDH"]

semester = "2026-2027-1"

path = Path(f"pdf/{semester}")
path.mkdir(parents=True, exist_ok=True)

for course in courses:
    try:
        response = rq.get(url + course + ".pdf")
        if response.status_code == 200:
            with open(f"pdf/{semester}/{course}.pdf", "wb") as f:
                f.write(response.content)
            print(f"Downloaded {course}.pdf")
        else:
            print(f"Failed to download {course}.pdf: Status code {response.status_code}")

    except Exception as e:
        print(f"Lỗi khi tải {course}.pdf: {e}")

    except ValueError as error:
        print(f"Lỗi nội dung {course}.pdf: {error}")

def download_pdf(semester):
    path = Path(f"pdf/{semester}")
    path.mkdir(parents=True, exist_ok=True)

    for course in courses:
        try:
            response = rq.get(url + course + ".pdf")
            if response.status_code == 200:
                with open(f"pdf/{semester}/{course}.pdf", "wb") as f:
                    f.write(response.content)
                print(f"Downloaded {course}.pdf")
            else:
                print(f"Failed to download {course}.pdf: Status code {response.status_code}")

        except Exception as e:
            print(f"Lỗi khi tải {course}.pdf: {e}")

        except ValueError as error:
            print(f"Lỗi nội dung {course}.pdf: {error}")


def download_pdf(course, semester):
    path = Path(f"pdf/{semester}")
    path.mkdir(parents=True, exist_ok=True)

    try:
        response = rq.get(url + course + ".pdf")
        if response.status_code == 200:
            with open(f"pdf/{semester}/{course}.pdf", "wb") as f:
                f.write(response.content)
            print(f"Downloaded {course}.pdf")
        else:
            print(f"Failed to download {course}.pdf: Status code {response.status_code}")

    except Exception as e:
        print(f"Lỗi khi tải {course}.pdf: {e}")

    except ValueError as error:
        print(f"Lỗi nội dung {course}.pdf: {error}")