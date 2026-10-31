# Hệ Thống Tối Ưu Thời Khóa Biểu (TimeTableManagement)

> **Học phần:** Lập trình ứng dụng mạng (Mã HP: `841438`) — Khoa Công nghệ Thông tin, Trường Đại học Sài Gòn (SGU)  
> **Đề tài 15:** Ứng dụng Client–Server hỗ trợ sinh viên tìm và tối ưu thời khóa biểu học kỳ  
> **Nhóm thực hiện:** Nhóm 17 — Học kỳ 1, Năm học 2026–2027

---

## 1. Giới thiệu tổng quan

**TimeTableManagement** là ứng dụng mạng theo mô hình **Client – Server** giao tiếp qua giao thức **TCP Socket** (cổng mặc định `9000`), hỗ trợ sinh viên chọn danh sách học phần mong muốn theo thứ tự ưu tiên và tự động tìm ra các phương án thời khóa biểu **không trùng lịch** (xét cả Thứ, Tiết và Tuần học), đồng thời xếp hạng theo các tiêu chí tối ưu do người dùng chọn:
- **Ít ngày đến trường nhất** (`minDays`)
- **Ưu tiên học buổi sáng** (`morningFirst` — tiết 1 đến tiết 5)
- **Tránh trống tiết giữa buổi** (`avoidGaps`)

Trường hợp không thể xếp đủ tất cả học phần đã chọn, hệ thống tự động xác định **học phần có độ ưu tiên thấp nhất trong số các học phần đang gây xung đột** để loại bớt rồi tìm lại phương án tốt nhất có thể, đồng thời thông báo rõ học phần bị loại.

### Phân công nhiệm vụ trong nhóm

| Thành viên | Vai trò & Hạng mục phụ trách |
| :--- | :--- |
| **Thái Anh (`Eser`)** | • **Hạ tầng & CSDL (`B1`):** Docker Compose (MySQL 8.0 + Adminer), thiết kế Schema 8 bảng, HikariCP Connection Pool & tầng DAO.<br>• **Công cụ dữ liệu (`tools/`):** Script Python bóc tách PDF Sổ tay đăng ký môn học sang CSV và nạp vào MySQL.<br>• **Module `common` (`B2`):** Khung truyền tải TCP (`FrameCodec`), Mã hóa lai RSA + AES-GCM (`CryptoUtil`), Giao thức (`Request`, `Response`, `Command`, `ErrorCode`) và bộ DTO.<br>• **Toàn bộ `backend` Server (`B3–B11`):** TCP Server đa luồng, quản lý phiên, xác thực BCrypt, Cache RAM, thuật toán Backtracking + Bitmask tối ưu TKB, `JobManager` (`ExecutorService`) báo tiến độ/hủy job và các lệnh Admin. |
| **Trân** | • **Nền tảng mạng phía Client (`C1`):** `NetworkClient` (kết nối TCP, bắt tay RSA/AES, luồng đọc nền nhận gói Push, ghép `Response` bằng `CompletableFuture`), màn hình Đăng nhập/Đăng ký/Cấu hình IP.<br>• **Giao diện Admin (`C2` - JavaFX):** Thống kê hệ thống, quản lý học kỳ/học phần/nhóm lớp/người dùng, tải file PDF theo từng chunk (`IMPORT_BEGIN` / `IMPORT_CHUNK` / `IMPORT_COMMIT`), xem báo cáo `ImportReport` và giám sát tiến trình. |
| **Quý** | • **Lưới thời khóa biểu (`D1` - JavaFX):** Component hiển thị lưới tuần (Thứ 2 – Chủ nhật × Tiết 1 – 12), phân biệt màu học phần và nhãn Lý thuyết (`LT`) / Thực hành (`TH`).<br>• **Giao diện Sinh viên (`D2–D7` - JavaFX):** Chọn học kỳ, tìm & sắp xếp thứ tự ưu tiên học phần, chọn tiêu chí tối ưu, thanh tiến độ `JOB_PROGRESS` + nút hủy job, xem/lưu/so sánh các phương án và xuất thời khóa biểu ra ảnh PNG. |

---

## 2. Công nghệ & Kỹ thuật sử dụng

- **Ngôn ngữ & Nền tảng:** Java 21 (Maven Multi-module: `common`, `backend`, `frontend`), Python 3.11+ (công cụ tiền xử lý dữ liệu PDF/CSV).
- **Giao diện người dùng (GUI):** JavaFX.
- **Truyền tải & Đa luồng:** TCP Socket kết hợp `ExecutorService` (Thread Pool) phục vụ nhiều Client và xử lý đồng thời nhiều tiến trình tính toán (`Job`).
- **Bảo mật kênh truyền (Hybrid Encryption):**
  - **Trao đổi khóa phiên:** Bất đối xứng **RSA 2048-bit** (`RSA/ECB/OAEPWithSHA-256AndMGF1Padding`).
  - **Mã hóa dữ liệu phiên:** Đối xứng **AES 256-bit chế độ GCM** (`AES/GCM/NoPadding`), sinh **12-byte IV ngẫu nhiên mới** cho từng gói tin kèm thẻ xác thực **128-bit GCM Auth Tag**. Mỗi kết nối Client sử dụng một khóa phiên AES riêng biệt.
- **Bảo mật mật khẩu:** Băm mật khẩu bằng **BCrypt** (không lưu mật khẩu dạng rõ).
- **Cơ sở dữ liệu:** MySQL 8.0 (chạy trên Docker), kết nối qua **HikariCP Connection Pool** và truy vấn bằng `PreparedStatement` chống SQL Injection.

---

## 3. Cấu trúc thư mục dự án

```text
TimeTableManagement/
├── docker-compose.yml                  # Cấu hình container MySQL 8.0 và Adminer
├── init-schema/
│   └── 01-schema.sql                   # Schema khởi tạo 8 bảng CSDL (UTF-8mb4)
├── csv/
│   └── 2026-2027-1/
│       └── thoi_khoa_bieu.csv          # Dữ liệu thời khóa biểu đã chuẩn hóa từ PDF
├── tools/
│   ├── processing_data.py              # Script bóc tách PDF Sổ tay ĐKMH -> CSV (hỗ trợ LT/TH & tuần học)
│   ├── import_tkb_mysql.py             # Script kiểm tra, chuẩn hóa CSV và nạp vào MySQL Docker
│   └── requirements.txt                # Danh sách thư viện Python (pdfplumber, mysql-connector-python...)
├── pom.xml                             # Maven Parent POM quản lý 3 module
├── common/                             # Module dùng chung cho cả Backend và Frontend
│   └── src/main/java/com/tkb/common/
│       ├── net/
│       │   └── FrameCodec.java         # Đọc/ghi khung 4-byte Big-Endian length + payload (tối đa 1 MiB)
│       ├── crypto/
│       │   └── CryptoUtil.java         # Mã hóa lai RSA-2048 OAEP & AES-256-GCM
│       ├── protocol/
│       │   ├── Command.java            # Enum 34 lệnh giao thức + phân quyền (NONE, USER, ADMIN)
│       │   ├── ErrorCode.java          # Enum 21 mã lỗi chuẩn hóa kèm thông báo tiếng Việt
│       │   ├── Request.java            # Đối tượng gói tin yêu cầu từ Client
│       │   └── Response.java           # Đối tượng gói tin phản hồi & gói đẩy (Push) từ Server
│       └── dto/
│           ├── SlotDto.java            # Buổi học (thứ, tiết bắt đầu, số tiết, phòng, chuỗi tuần học)
│           ├── SectionDto.java         # Nhóm lớp học phần (giữ đầy đủ LT/TH, nhóm, tổ TH, danh sách buổi)
│           ├── PlanDto.java            # Phương án TKB hoàn chỉnh, điểm số, chỉ số (metrics) & môn bị loại
│           ├── JobDto.java             # Trạng thái tiến trình OPTIMIZE / IMPORT
│           ├── UserDto.java            # Thông tin tài khoản người dùng
│           └── ImportReport.java       # Báo cáo kết quả kiểm tra/nhập dữ liệu PDF
├── backend/                            # Module Server (Đa luồng, Giải thuật, DAO, Quản lý phiên)
│   └── src/main/java/com/tkb/backend/
└── frontend/                           # Module Client JavaFX (Trang User của Quý & Trang Admin của Trân)
    └── src/main/java/com/tkb/frontend/
```

---

## 4. Hướng dẫn khởi chạy Cơ sở dữ liệu & Nạp dữ liệu học kỳ

### Bước 1: Khởi chạy MySQL và Adminer trên Docker

Tại thư mục gốc của dự án, chạy lệnh:

```bash
docker compose up -d
```

- **MySQL** lắng nghe tại cổng `3306` (Database: `tkb_db`).
- **Adminer** (giao diện quản lý DB trên trình duyệt) truy cập tại `http://localhost:8080`.
- File `init-schema/01-schema.sql` sẽ tự động tạo 8 bảng: `semester`, `course`, `class_section`, `schedule_slot`, `users`, `timetable_plan`, `timetable_plan_section`, `timetable_plan_dropped_course`.

### Bước 2: Cài đặt môi trường Python (xử lý PDF & CSV)

```bash
python3 -m venv venv
source venv/bin/activate
# pip install pdfplumber mysql-connector-python
# pip freeze > requirements.txt
pip install requirements.txt
```

### Bước 3: Bóc tách PDF Sổ tay đăng ký môn học sang CSV

Đặt các file PDF thời khóa biểu vào thư mục `pdf/<nam_hoc>/<hoc_ky>/` và chạy:

```bash
python tools/processing_data.py
```

Script sẽ nhận diện tọa độ các cột trên từng trang PDF, tự động:
- Xác định cột **Tổ hợp thực hành (`to_th`)** để gán nhãn loại lớp là **`TH`** (Thực hành) hoặc **`LT`** (Lý thuyết).
- Trích xuất chính xác chuỗi **Tuần học (`tuan_hoc`)** (giữ nguyên các ký tự `-` hoặc khoảng trắng đầu chuỗi để biểu thị tuần nghỉ/tuần bắt đầu học).
- Xuất ra file `csv/<nam_hoc>-<hoc_ky>/thoi_khoa_bieu.csv` gồm 12 cột:
  `ma_mh,ten_mh,so_tin_chi,giang_vien,nhom,to_th,loai,thu,tiet_bd,so_tiet,phong,tuan_hoc`

### Bước 4: Chuẩn hóa CSV và nạp vào MySQL Docker

```bash
python tools/import_tkb_mysql.py --semester 2026-2027-1
```

Script thực hiện:
1. Kiểm tra tính hợp lệ của từng dòng CSV, loại bỏ các buổi không có lịch cố định (`thu = 0`) hoặc dòng trùng lặp.
2. Ghi đè lại file CSV chuẩn hóa 12 cột.
3. Nạp dữ liệu vào 4 bảng `semester`, `course`, `class_section`, `schedule_slot` trong một Transaction an toàn.

---

## 5. Đặc tả Giao thức mạng & Quy ước Nghiệp vụ

### 5.1. Đóng khung và Bắt tay bảo mật (Handshake)

1. **Khung gói tin (`FrameCodec`):** Mỗi gói tin trên luồng TCP gồm `[4 byte độ dài Big-Endian int] + [Payload]`. Kích thước tối đa `1 MiB` (`1,048,576 bytes`).
2. **Bước 1 — `HELLO` (Server $\rightarrow$ Client, chưa mã hóa):**
   ```json
   {"type": "HELLO", "protocolVersion": 1, "publicKey": "<Base64 RSA-2048 X.509 PublicKey>"}
   ```
3. **Bước 2 — `KEY_EXCHANGE` (Client $\rightarrow$ Server, chưa mã hóa):**
   Client sinh khóa phiên `AES-256` ngẫu nhiên bằng `SecureRandom`, mã hóa bằng `RSA/ECB/OAEPWithSHA-256AndMGF1Padding` với PublicKey của Server:
   ```json
   {"type": "KEY_EXCHANGE", "encryptedKey": "<Base64 RSA-Encrypted AES-256 Key>"}
   ```
4. **Bước 3 — `READY` (Server $\rightarrow$ Client, đã mã hóa AES-256-GCM):**
   ```json
   {"type": "READY"}
   ```
   Từ sau bước này, mọi gói tin `Request`, `Response` và `Push` đều được mã hóa theo cấu trúc:  
   `Payload = [12 byte IV ngẫu nhiên] + [Ciphertext] + [16 byte GCM Auth Tag]`.

### 5.2. Quy ước dữ liệu & Kiểm tra trùng lịch

- **Thứ trong tuần (`dayOfWeek`):** `2` (Thứ Hai) đến `7` (Thứ Bảy), `8` (Chủ Nhật).
- **Tiết học (`startPeriod`, `periodCount`):**
  - Buổi sáng: **Tiết 1 – 5**.
  - Buổi chiều: **Tiết 6 – 10** (hỗ trợ ngoại lệ tới tiết 11 – 12).
- **Phân loại nhóm lớp (`type` & `labGroup`):**
  - `type = "LT"`: Lớp lý thuyết (`labGroup = ""`).
  - `type = "TH"`: Tổ thực hành (`labGroup = "01"`, `"02"`, ...).
- **Xét trùng lịch theo Tuần học (`weeks`):**
  - Hai buổi học cùng thứ và giao nhau về tiết học chỉ bị coi là **trùng lịch** nếu có ít nhất 1 tuần học trùng nhau (tại cùng vị trí tuần thứ $i$, cả hai chuỗi đều khác `'-'` và khác `' '`).
  - Ví dụ: Nhóm học tuần chẵn (`-2-4-6-8-0-2------`) và nhóm học tuần lẻ (`1-3-5-7-9-1-------`) cùng Thứ 4 tiết 1–5 **không bị trùng lịch**.

---

## 6. Trạng thái tiến độ hiện tại

- [x] **Hạ tầng Docker & Schema CSDL (`B1.1`, `B1.2`):** Hoàn thành `docker-compose.yml` và `01-schema.sql` (8 bảng, hỗ trợ nhiều học kỳ, phân biệt LT/TH và lưu chuỗi tuần học `VARCHAR(64)`).
- [x] **Công cụ xử lý dữ liệu (`tools/`):** Hoàn thành `processing_data.py` và `import_tkb_mysql.py`, đã nạp thành công 3.881 dòng dữ liệu học kỳ `2026-2027-1` vào MySQL trong Docker.
- [x] **Module `common` (`B2.1` – `B2.4`):** Đã hoàn thiện 12 file mã nguồn (`FrameCodec`, `CryptoUtil`, `Command`, `ErrorCode`, `Request`, `Response`, `SlotDto`, `SectionDto`, `PlanDto`, `JobDto`, `UserDto`, `ImportReport`).
- [ ] **Bước tiếp theo (`B1.4` – `B1.6`):** Cấu hình `HikariCP` Connection Pool và viết các lớp `DAO` (`SemesterDao`, `CourseDao`, `SectionDao`, `UserDao`, `PlanDao`) trong module `backend`.
- [ ] **Server Socket & Xác thực (`B3`, `B4`):** Xây dựng TCP Server đa luồng, bắt tay RSA/AES, Dispatcher phân quyền và đăng nhập BCrypt.
- [ ] **Thuật toán Tối ưu TKB & Quản lý Job (`B5`, `B6`, `B7`):** Cache RAM, Backtracking + Bitmask, chấm điểm 3 tiêu chí, báo tiến độ `JOB_PROGRESS` và hủy job.
