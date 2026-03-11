## Về viết code
* AI chỉ sửa code thuộc class, không sửa code liên quan đến class khác
* AI đọc databse -> tuân thủ hexagonal + DDD
* Sử dụng 2 file trong /shared khi viết api để cùng định dạng

## Về commit code 
* tạo nhánh riêng: git checkout -b <tên nhánh>
* code không lỗi -> merge main vào nhánh của mình -> không xung đột -> merge nhánh của mình vào main

## Về docker
* cd /docker 
* Chạy docker: docker compose up -d
### Bị lỗi liên quan đến database
* Dừng docker: docker compose down -v
* Xóa folder /data trong /docker
* Chạy lại docker

## Chạy local/prod
* Local: chạy bằng nút Run vào hàm `main` trong IDE.
* Local mặc định dùng profile `dev` từ `src/main/resources/application.properties` (`spring.profiles.active=dev`).
* Deploy dùng profile `prod` bằng biến môi trường `SPRING_PROFILES_ACTIVE=prod`.
* Cấu hình local nằm trong `src/main/resources/application-dev.properties`.
* Cấu hình deploy nằm trong `src/main/resources/application-prod.properties`.

## Ví dụ notification khi user xử lý hoạt động cần quyền admin
* Đã thêm ví dụ cho luồng moderation admin (`POST`/`PAPER`): sau khi `APPROVE/REJECT` thành công, hệ thống gửi email cho:
  * toàn bộ tài khoản có role `ADMIN`
  * user vừa thực hiện action (kể cả user được admin ủy quyền quyền xử lý)
* Bật/tắt qua biến:
  * `APP_NOTIFICATION_ADMIN_ACTIVITY_EMAIL_ENABLED=true|false`
* SMTP cần cấu hình:
  * `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`
* Tuỳ chọn:
  * `APP_NOTIFICATION_ADMIN_ACTIVITY_EMAIL_FROM`
  * `APP_NOTIFICATION_ADMIN_ACTIVITY_EMAIL_SUBJECT_PREFIX`
