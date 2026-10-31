# TimeTableManagement
#ToiUuHoaTKB

## Thái Anh (Backend):

### Docker

```bash

docker compose up -d                                                : khởi chạy docker để kết nối đến database
docker compose down                                                 : tắt docker nhưng không xóa dữ liệu
docker compose down -v                                              : tắt docker nhưng xóa hết dữ liệu
docker exec -it tkb-mysql mysql -h 127.0.0.1 -u timetable_user -p   : sử dụng code sql để tra cứu, phải nhập trong file .env để có thể vào

```

### Maven

```bash

mvn clean compile : tự động tải xuống các thư viện nếu như lần đầu hoặc có thay đổi trong file pom.xml

```

### Thư viện python

```bash

pip install requirements.txt : tải các thư viện cần thiết của python trước khi chạy file python cũng như backend
```

#### Cách chạy backend

```bash

docker compose up -d
mvn clean compile
# (chạy file main tiếp theo)

python tools/import_tkb_mysql.py csv/2026-2027-1/thoi_khoa_bieu.csv # nạp file csv hiện tại vào mysql

```


#### Dùng lệnh để vào được sql query

```bash
docker exec -it tkb-mysql mysql -u timetable_user -p
```

## Ngọc Trân (Frontend):


## Quý (Frontend):

### Cấu trúc frontend

- `frontend/user/` – giao diện dành cho sinh viên (package `com.tkb.frontend.user`)
- `frontend/admin/` – giao diện dành cho quản trị viên (package `com.tkb.frontend.admin`, thêm khi cần)

Cả hai thư mục nguồn được biên dịch chung trong module `frontend`.

### Cách chạy: mvn -pl frontend javafx:run