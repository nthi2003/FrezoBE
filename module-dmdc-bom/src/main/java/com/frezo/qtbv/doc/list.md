# Quản lý tài sản (QLTS) - Hướng dẫn sử dụng Category

## Tổng quan

Từ 2026-07: module **Quản lý tài sản** đã tách thành entity riêng `asset` (xem
`Asset.java` + `AssetController.java`). Tuy nhiên các **loại tài sản** (Laptop, Bàn,
Ghế, Xe, ...) vẫn dùng chung bảng `categories` với `group_code = 'LoaiTaiSan'` để tận
dụng trang `/admin/category-management` có sẵn.

> **Migration ⚠️**: các seed cũ dùng `group_code = 'QLTS'` đã được `AssetDataInitializer`
> tự động chuyển sang `LoaiTaiSan` khi khởi động. Không dùng `QLTS` cho seed mới.

## Cấu trúc nhóm (CategoryGroup)

| STT | Code | Tên nhóm | Mục đích |
|-----|------|----------|----------|
| 1 | LoaiTaiSan | Loại Tài Sản | Danh mục loại tài sản (LAPTOP, DESK, CHAIR, VEHICLE...) — dùng cho dropdown "Loại" khi tạo Asset |
| 2 | ChucDanh   | Chức Danh    | Giám đốc, Trưởng phòng, Nhân viên... — **canonical** (QA-QLNS-001). Legacy seed `TITLE` migrate → `ChucDanh` lúc boot (`category_data.sql`). Query: `GET /qtht/category?groupCode=ChucDanh` (`type` = alias). Item codes mẫu: `TTL_*`. |
| 3 | DonVi      | Đơn Vị       | Phòng Kinh doanh, Phòng Kỹ thuật... |
| 4 | DanhMucSP  | Danh Mục SP  | Nhóm sản phẩm (dùng cho module product) |

## Cơ chế query

### 1. Lấy danh sách loại tài sản (dropdown)

FE:

```ts
useCategories('LoaiTaiSan')
```

BE query tương đương:

```sql
SELECT * FROM categories
WHERE group_code = 'LoaiTaiSan'
  AND is_deleted = false
ORDER BY order_index;
```

### 2. Lấy danh sách tài sản thực (Asset)

Không dùng `categories` — đã có API riêng:

```
GET /qlts/assets?keyword=&status=&categoryCode=&assignedPersonId=&page=&size=
```

Trong đó `categoryCode` là `code` của một Category thuộc group `LoaiTaiSan` (vd `LAPTOP`).
