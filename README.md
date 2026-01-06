# CarRental - Hệ thống Thuê Xe

## Hướng dẫn Tạo và Quản lý Tài khoản Admin/Staff

### Tài khoản Mặc định

Khi khởi động ứng dụng lần đầu tiên, hệ thống sẽ tự động tạo 2 tài khoản mặc định:

#### 1. Tài khoản Admin
- **Tên đăng nhập**: `admin`
- **Mật khẩu**: `admin123`
- **Vai trò**: ADMIN
- **Quyền**: Quản lý toàn bộ hệ thống

#### 2. Tài khoản Staff
- **Tên đăng nhập**: `staff`
- **Mật khẩu**: `staff123`
- **Vai trò**: STAFF
- **Quyền**: Quản lý xe, trả xe, xác minh khách hàng

> **Lưu ý Bảo mật**: Mật khẩu mặc định chỉ hiển thị trong tài liệu này để hỗ trợ việc thiết lập ban đầu. Hệ thống sẽ KHÔNG in mật khẩu ra console khi khởi động, chỉ in thông báo tạo tài khoản thành công với tên đăng nhập. Hãy lưu giữ thông tin này ở nơi an toàn và đổi mật khẩu ngay sau khi đăng nhập lần đầu.

### Đổi Mật khẩu Mặc định

**⚠️ QUAN TRỌNG**: Sau khi đăng nhập lần đầu, bạn **PHẢI** đổi mật khẩu mặc định để bảo mật hệ thống.

### Tạo Tài khoản Admin/Staff Mới

#### Cách 1: Sử dụng Database (MySQL)

1. Kết nối vào MySQL database:
```bash
mysql -u root -p
use carrental;
```

2. Tạo tài khoản Admin mới:
```sql
INSERT INTO users (username, password, role, full_name, enabled, verified, updated_at)
VALUES ('admin_new', '$2a$10$YOUR_BCRYPT_HASHED_PASSWORD', 'ADMIN', 'Admin Name', 1, 1, NOW());
```

3. Tạo tài khoản Staff mới:
```sql
-- Lưu ý: Đảm bảo station với id=1 đã tồn tại trong bảng stations trước khi chạy lệnh này
INSERT INTO users (username, password, role, full_name, enabled, verified, station_id, updated_at)
VALUES ('staff_new', '$2a$10$YOUR_BCRYPT_HASHED_PASSWORD', 'STAFF', 'Staff Name', 1, 1, 1, NOW());
```

**Lưu ý**: Để tạo mật khẩu BCrypt hash, bạn có thể sử dụng công cụ online hoặc code Java:

```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hashedPassword = encoder.encode("your_password");
System.out.println(hashedPassword);
```

#### Cách 2: Sửa đổi UserDataLoader

1. Mở file `CarRental/src/main/java/config/UserDataLoader.java`

2. Thêm code tạo tài khoản mới vào phương thức `run()`:

```java
// Tạo admin mới
if (userRepo.findByUsername("admin2") == null) {
    User admin = new User();
    admin.setUsername("admin2");
    admin.setPassword(passwordEncoder.encode("password123"));
    admin.setRole("ADMIN");
    admin.setFullName("Admin Two");
    admin.setEnabled(true);
    admin.setVerified(true);
    userRepo.save(admin);
    System.out.println("Admin account created - Username: admin2");
}

// Tạo staff mới với station
if (userRepo.findByUsername("staff2") == null) {
    User staff = new User();
    staff.setUsername("staff2");
    staff.setPassword(passwordEncoder.encode("password123"));
    staff.setRole("STAFF");
    staff.setFullName("Staff Two");
    staff.setStationId(1L); // Gán vào trạm số 1
    staff.setEnabled(true);
    staff.setVerified(true);
    userRepo.save(staff);
    System.out.println("Staff account created - Username: staff2");
}
```

3. Khởi động lại ứng dụng để tạo tài khoản mới.

### Phân quyền theo Vai trò

#### ADMIN
- Truy cập: `/admin/**`
- API: `/api/vehicles/admin/**`, `/api/stations/admin/**`, `/api/rental/admin/**`
- Chức năng:
  - Quản lý toàn bộ xe
  - Quản lý trạm
  - Quản lý tất cả đơn thuê
  - Quản lý người dùng
  - Xem báo cáo và thống kê

#### STAFF
- Truy cập: `/staff/**`
- API: `/api/staff/**`, `/api/staff/return/**`
- Chức năng:
  - Xác minh tài liệu khách hàng
  - Giao xe và nhận xe
  - Kiểm tra tình trạng xe
  - Quản lý xe tại trạm được gán

#### USER (Khách hàng)
- Truy cập: `/datxe`, `/thanhtoan`, `/lichsuthue`, `/user-hosocanhan`
- Chức năng:
  - Đặt xe
  - Thanh toán
  - Xem lịch sử thuê
  - Quản lý hồ sơ cá nhân

### Cấu hình Database

Đảm bảo MySQL đang chạy và cấu hình đúng trong `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/carrental?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
spring.jpa.hibernate.ddl-auto=update
```

### Khởi động Ứng dụng

```bash
# Build project
mvn clean package

# Run application
mvn spring-boot:run
```

Hoặc chạy file JAR:
```bash
java -jar target/CarRental-0.0.1-SNAPSHOT.jar
```

### Xác minh Tài khoản đã được Tạo

1. Truy cập: http://localhost:8080/login

2. Đăng nhập với:
   - **Admin**: username `admin`, password `admin123`
   - **Staff**: username `staff`, password `staff123`

3. Hoặc kiểm tra trong database:
```sql
SELECT id, username, role, full_name, enabled, verified FROM users WHERE role IN ('ADMIN', 'STAFF');
```

### Bảo mật

1. **Đổi mật khẩu mặc định** ngay sau khi đăng nhập lần đầu
2. Sử dụng mật khẩu mạnh (ít nhất 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt)
3. Không chia sẻ thông tin đăng nhập
4. Với môi trường production, sử dụng biến môi trường cho database password:
```properties
spring.datasource.password=${DB_PASSWORD}
```

### Xử lý Sự cố

#### Không thể đăng nhập
- Kiểm tra username và password
- Kiểm tra cột `enabled` trong database (phải là `1`)
- Kiểm tra MySQL server đang chạy

#### Không có quyền truy cập
- Kiểm tra cột `role` trong database (phải là `ADMIN` hoặc `STAFF`)
- Đảm bảo role được ghi hoa

#### Tài khoản mặc định không được tạo
- Kiểm tra log khi khởi động ứng dụng
- Kiểm tra kết nối database
- Chạy lại ứng dụng

### Hỗ trợ

Nếu gặp vấn đề, hãy kiểm tra:
1. Log của ứng dụng khi khởi động
2. Kết nối MySQL database
3. File cấu hình `application.properties`
4. Bảng `users` trong database

---

## Account Creation and Management Guide (English)

### Default Accounts

When starting the application for the first time, the system will automatically create 2 default accounts:

#### 1. Admin Account
- **Username**: `admin`
- **Password**: `admin123`
- **Role**: ADMIN
- **Permissions**: Full system management

#### 2. Staff Account
- **Username**: `staff`
- **Password**: `staff123`
- **Role**: STAFF
- **Permissions**: Vehicle management, returns, customer verification

> **Security Note**: Default passwords are only displayed in this documentation to support initial setup. The system will NOT print passwords to the console during startup, only success messages with usernames. Please keep this information secure and change passwords immediately after first login.

### Change Default Password

**⚠️ IMPORTANT**: After first login, you **MUST** change the default password for security.

### Creating New Admin/Staff Accounts

#### Method 1: Using Database (MySQL)

1. Connect to MySQL database:
```bash
mysql -u root -p
use carrental;
```

2. Create new Admin account:
```sql
INSERT INTO users (username, password, role, full_name, enabled, verified, updated_at)
VALUES ('admin_new', '$2a$10$YOUR_BCRYPT_HASHED_PASSWORD', 'ADMIN', 'Admin Name', 1, 1, NOW());
```

3. Create new Staff account:
```sql
-- Note: Ensure station with id=1 exists in the stations table before running this command
INSERT INTO users (username, password, role, full_name, enabled, verified, station_id, updated_at)
VALUES ('staff_new', '$2a$10$YOUR_BCRYPT_HASHED_PASSWORD', 'STAFF', 'Staff Name', 1, 1, 1, NOW());
```

#### Method 2: Modify UserDataLoader

1. Open `CarRental/src/main/java/config/UserDataLoader.java`

2. Add code to create new accounts in the `run()` method

3. Restart the application to create new accounts

### Starting the Application

```bash
mvn spring-boot:run
```

### Verify Accounts Created

1. Access: http://localhost:8080/login

2. Login with:
   - **Admin**: username `admin`, password `admin123`
   - **Staff**: username `staff`, password `staff123`

3. Or check in database:
```sql
SELECT id, username, role, full_name, enabled, verified FROM users WHERE role IN ('ADMIN', 'STAFF');
```
