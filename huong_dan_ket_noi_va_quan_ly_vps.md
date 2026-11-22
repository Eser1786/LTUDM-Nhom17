# Hướng dẫn kết nối và quản lý VPS CloudFly -- TKB Management

## 1. Thông tin VPS

  -----------------------------------------------------------------------------
  Thông tin                           Giá trị
  ----------------------------------- -----------------------------------------
  Nhà cung cấp                        CloudFly

  IP                                  `103.82.20.223`

  Hệ điều hành                        Ubuntu 22.04

  SSH user                            `root`

  SSH port                            `22`

  Backend TCP                         `9000`

  MySQL                               Docker, chỉ bind nội bộ `127.0.0.1:3306`

  Thư mục dự án                       `/opt/projects/LTUDM-Nhom17`

  Tên systemd service                 `tkb-backend`

  Tên MySQL container                 `tkb-mysql`

  SSH private key trên Windows        `%USERPROFILE%\\.ssh\\cloudfly_ed25519`
  -----------------------------------------------------------------------------

> Không chia sẻ file private key `cloudfly_ed25519`, mật khẩu MySQL hoặc
> nội dung file `.env`.

## 2. Kết nối SSH từ Windows

Mở **PowerShell** trên máy tính:

``` powershell
ssh -i "$env:USERPROFILE\.ssh\cloudfly_ed25519" root@103.82.20.223
```

Nếu SSH hỏi xác nhận fingerprint ở lần kết nối đầu tiên, hãy kiểm tra
fingerprint từ nhà cung cấp VPS trước khi xác nhận.

Sau khi kết nối thành công, terminal sẽ chuyển sang máy Ubuntu.

### Thoát khỏi VPS

``` bash
exit
```

## 3. Kiểm tra kết nối Backend từ Windows

Mở một cửa sổ PowerShell:

``` powershell
Test-NetConnection 103.82.20.223 -Port 9000
```

Nếu kết quả có:

``` text
TcpTestSucceeded : True
```

thì cổng TCP `9000` có thể truy cập từ máy tính.

## 4. Di chuyển đến thư mục dự án

Sau khi SSH vào VPS:

``` bash
cd /opt/projects/LTUDM-Nhom17
```

Kiểm tra thư mục hiện tại:

``` bash
pwd
ls -la
```

## 5. Quản lý Java Backend bằng systemd

### Kiểm tra trạng thái

``` bash
systemctl status tkb-backend
```

### Khởi động Backend

``` bash
systemctl start tkb-backend
```

### Dừng Backend

``` bash
systemctl stop tkb-backend
```

### Khởi động lại Backend

``` bash
systemctl restart tkb-backend
```

### Bật tự khởi động cùng VPS

``` bash
systemctl enable tkb-backend
```

### Xem 100 dòng log gần nhất

``` bash
journalctl -u tkb-backend -n 100 --no-pager
```

### Theo dõi log trực tiếp

``` bash
journalctl -u tkb-backend -f
```

Nhấn `Ctrl + C` để thoát chế độ theo dõi log; thao tác này không dừng
Backend.

### Kiểm tra cổng 9000

``` bash
ss -lntp | grep ':9000'
```

## 6. Quản lý MySQL Docker

### Xem trạng thái các container

``` bash
docker ps -a
```

### Xem log MySQL

``` bash
docker logs --tail 100 tkb-mysql
```

### Theo dõi log MySQL trực tiếp

``` bash
docker logs -f tkb-mysql
```

Nhấn `Ctrl + C` để thoát xem log.

### Khởi động MySQL

``` bash
docker start tkb-mysql
```

### Dừng MySQL

``` bash
docker stop tkb-mysql
```

### Khởi động lại MySQL

``` bash
docker restart tkb-mysql
```

### Kết nối vào database

``` bash
docker exec -it tkb-mysql mysql -u tkb_user -p tkb_db
```

Nhập mật khẩu MySQL khi được hỏi. Ký tự mật khẩu sẽ không hiển thị khi
gõ.

Một số lệnh SQL hữu ích:

``` sql
SHOW TABLES;
```

Thoát MySQL:

``` sql
exit;
```

> Không mở cổng MySQL `3306` trực tiếp ra Internet. Cấu hình hiện tại sử
> dụng kết nối nội bộ trên VPS.

## 7. Chạy Python trong môi trường ảo

Di chuyển đến thư mục dự án:

``` bash
cd /opt/projects/LTUDM-Nhom17
```

Kích hoạt virtual environment:

``` bash
source .venv/bin/activate
```

Kiểm tra Python:

``` bash
which python
python --version
```

Thoát virtual environment:

``` bash
deactivate
```

Có thể gọi Python trực tiếp mà không cần kích hoạt môi trường:

``` bash
.venv/bin/python --version
```

## 8. Các lệnh xử lý dữ liệu thời khóa biểu

### Tải PDF từ website SGU

Ví dụ học kỳ `2026-2027-1`:

``` bash
cd /opt/projects/LTUDM-Nhom17

.venv/bin/python tools/pdf_download.py \
  --semester 2026-2027-1
```

### Kiểm tra số lượng PDF đã tải

``` bash
find pdf/2026-2027-1/ -maxdepth 1 -type f -name '*.pdf' | wc -l
```

### Xem danh sách PDF

``` bash
ls -lh pdf/2026-2027-1/
```

### Bóc tách PDF và chỉ tạo CSV

``` bash
.venv/bin/python tools/processing_data_v2.py \
  --semester 2026-2027-1 \
  --csv-only
```

### Bóc tách PDF và thực hiện bước nhập MySQL theo cấu hình script

``` bash
.venv/bin/python tools/processing_data_v2.py \
  --semester 2026-2027-1
```

> Lệnh không có `--csv-only` có thể ghi dữ liệu vào MySQL. Trước khi
> chạy lại trên dữ liệu đang sử dụng, cần kiểm tra cơ chế thêm mới, cập
> nhật và chống trùng của script để tránh nhập lặp hoặc ghi đè ngoài ý
> muốn.

### Kiểm tra CSV được tạo

``` bash
ls -lh csv/2026-2027-1/
```

Đếm số dòng trong CSV (bao gồm dòng tiêu đề nếu có):

``` bash
wc -l csv/2026-2027-1/thoi_khoa_bieu.csv
```

## 9. Build lại và triển khai Java Backend

Chỉ thực hiện khi đã thay đổi mã nguồn Java và cần cập nhật JAR.

``` bash
cd /opt/projects/LTUDM-Nhom17

MAVEN_OPTS="-Xmx512m" mvn -pl backend -am clean package -DskipTests
```

Kiểm tra JAR:

``` bash
ls -lh backend/target/*.jar
```

Sau khi build thành công, khởi động lại service để chạy JAR mới:

``` bash
systemctl restart tkb-backend
```

Kiểm tra trạng thái và log:

``` bash
systemctl status tkb-backend
journalctl -u tkb-backend -n 100 --no-pager
```

## 10. Các lệnh kiểm tra nhanh khi Frontend không tải được dữ liệu

Chạy lần lượt trên VPS:

``` bash
# 1. Kiểm tra Java Backend
systemctl status tkb-backend

# 2. Kiểm tra cổng TCP
ss -lntp | grep ':9000'

# 3. Kiểm tra MySQL container
docker ps -a

# 4. Kiểm tra log Backend
journalctl -u tkb-backend -n 150 --no-pager

# 5. Kiểm tra log MySQL
docker logs --tail 100 tkb-mysql
```

Từ Windows, kiểm tra cổng Backend:

``` powershell
Test-NetConnection 103.82.20.223 -Port 9000
```

Nếu User đăng nhập được nhưng dữ liệu trống, hãy kiểm tra log Backend
đúng thời điểm đăng nhập và mở chức năng bị lỗi. Đăng nhập thành công
không đồng nghĩa mọi truy vấn dữ liệu sau đăng nhập đều thành công.

## 11. Lưu ý bảo mật và vận hành

-   Giữ kín private key SSH và file `.env`.
-   Không gửi mật khẩu database, khóa riêng hoặc thông tin bí mật vào
    cuộc trò chuyện hay kho mã nguồn công khai.
-   Không mở MySQL `3306` ra Internet nếu không có nhu cầu và biện pháp
    bảo vệ phù hợp.
-   Trước khi bật UFW, phải cho phép SSH port `22` trước để tránh tự
    khóa quyền truy cập VPS. Hiện không nên bật UFW một cách tùy tiện
    khi chưa kiểm tra các quy tắc tường lửa.
-   Nên thay mật khẩu Admin mặc định nếu dự án còn sử dụng thông tin
    đăng nhập mặc định.
-   Nên tạo tài khoản Linux riêng, ít quyền hơn `root` để chạy dịch vụ
    trong môi trường triển khai lâu dài.
-   Trước khi chạy script nhập dữ liệu nhiều lần, cần xác minh script có
    xóa, cập nhật hay chống trùng dữ liệu như thế nào.

------------------------------------------------------------------------

## Lệnh quan trọng nhất

Kết nối từ Windows bằng PowerShell:

``` powershell
ssh -i "$env:USERPROFILE\.ssh\cloudfly_ed25519" root@103.82.20.223
```

Sau khi đăng nhập:

``` bash
cd /opt/projects/LTUDM-Nhom17
```
