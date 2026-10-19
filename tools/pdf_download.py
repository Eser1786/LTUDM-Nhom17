import requests as rq

url = "https://daotao.sgu.edu.vn/images/pdf/sotaydkmh/261/"

courses = ["DKD", "DCM", "DCV", "DCT", "DDL", "DGD", "DGM", "DGT", "DKE", "DDU", "DKM", "DTT", "DKQ", "DKI", "DDE", "DDV", "DKP", "DLU", "DAN", "DQG", "DQK", "DHA", "DQV", "DQT", "DNH", "DDI", "DHO", "DKH", "DSU", "DLD", "DMI", "DVA", "DSI", "DSA", "DTO", "DLI", "DTN", "DTL", "DTU", "DNT", "DVI", "DLS", "DDH"]

semester = "2026-2027-1"

for course in courses:
    try:
        response = rq.get(url + course + ".pdf")
        if response.status_code == 200:
            with open(f"pdf/{course}_{semester}.pdf", "wb") as f:
                f.write(response.content)
            print(f"Downloaded {course}.pdf")
        else:
            print(f"Failed to download {course}.pdf: Status code {response.status_code}")

    except Exception as e:
        print(f"Lỗi khi tải {course}.pdf: {e}")

    except ValueError as error:
        print(f"Lỗi nội dung {course}.pdf: {error}")
