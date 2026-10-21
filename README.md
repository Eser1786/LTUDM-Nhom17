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

#### Cách chạy backend

```bash

docker compose up -d
mvn clean compile
(chạy file main tiếp theo)

```
#### Dùng lệnh để vào được sql query

```bash
docker exec -it tkb-mysql mysql -u timetable_user -p
```

## Ngọc Trân (Frontend):


## Quý (Frontend):