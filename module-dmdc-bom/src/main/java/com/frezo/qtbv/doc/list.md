# Quản lý tài sản (QLTS) - Hướng dẫn sử dụng Category

## Tổng quan

Hệ thống sử dụng bảng `categories` để lưu trữ các danh mục (tài sản, thiết bị, v.v...). Mỗi danh mục thuộc về một nhóm (`CategoryGroup`) được xác định qua `group_code`.

## Cấu trúc nhóm (CategoryGroup)

| STT | Code | Tên nhóm | cat_group | Mục đích |
|-----|------|----------|-----------|----------|
| 1 | QLTS | Quản lý tài sản | 1 | Nhóm chứa các danh mục tài sản |
| 2 | ChucDanh | Chức danh | 2 | Nhóm chứa các chức danh (Giám đốc, Trưởng phòng...) |
| 3 | DonVi | Đơn vị | 3 | Nhóm chứa các đơn vị (Phòng Kinh doanh, Phòng Kỹ thuật...) |
| 4 | LoaiTaiSan | Loại tài sản | 4 | Nhóm chứa loại tài sản (Máy tính, Xe, Bàn ghế...) |

## Cơ chế query

### 1. Lấy danh sách tài sản (View tài sản)

Khi muốn hiển thị danh sách tài sản cho module **Quản lý tài sản**, cần filter với `group_code = 'QLTS'`:

```sql
-- Query lấy tất cả tài sản
SELECT * FROM categories 
WHERE group_code = 'QLTS' 
  AND is_deleted = 0
ORDER BY order_index;