# Frezo Backend — module-dmdc-bom (Danh mục dùng chung + QLTS + Khấu hao)

> Module **DMDC**: bảng danh mục dùng chung (`categories` / `category_group`) — gồm Issuer, Signer, Chức danh, Địa bàn, Ngành, Đơn vị tính, UX popup, Loại tài sản, Bậc lương — cộng **Quản lý tài sản (QLTS)** và **khấu hao TSCĐ**.
> Đọc **cùng** [DATABASE_STANDARD.md](../DATABASE_STANDARD.md) · [module-qtht-bom.md](./module-qtht-bom.md) (Person/Org dùng cho gán tài sản).

Package controller: `com.frezo.dmdc`. Package domain/entity/service (legacy): `com.frezo.qtbv`. Module Maven: `module-dmdc-bom`.

---

## 1. Phạm vi & mục đích

| Domain | Sở hữu | API chính |
|--------|--------|-----------|
| **Danh mục dùng chung** | `categories` + `category_group` | `/qtht/category` |
| **UX Popup** | Resolve template từ `groupCode=UX_POPUP` | `/qtht/ux-popups/{eventCode}` |
| **QLTS** | `asset`, `asset_assignment`, `asset_transfer_request` | `/qlts/assets` |
| **Khấu hao** | `depreciation_schedule`, `depreciation_posting` | `/asset/depreciation` (+ alias `/qtbv/depreciation`) |

**Không** có entity riêng `Issuer` / `Signer` / `Title` / `Location` / `Industry`. Các khái niệm đó là **dòng** trong `categories`, phân biệt bằng `group_code`.

Seed master data (ISSUER, SIGNER, …): `module-auth-bom/src/main/resources/data/category_data.sql`.  
Seed LoaiTaiSan / SalaryBand / demo asset: initializer trong chính module này.

---

## 2. Class map

### 2.1 Controllers (`com.frezo.dmdc.controller`)

| Class | Base path | Vai trò |
|-------|-----------|---------|
| `CategoryController` | `/qtht/category` | CRUD danh mục theo `groupCode` |
| `UxPopupController` | `/qtht/ux-popups` | Resolve popup theo event code |
| `AssetController` | `/qlts/asset` **và** `/qlts/assets` | QLTS lifecycle + phiếu duyệt |
| `DepreciationController` | `/qtbv/depreciation` **và** `/asset/depreciation` | Lịch & post khấu hao |

Permission key khấu hao canonical: `/asset/depreciation/...`.

### 2.2 Domain services (`com.frezo.qtbv`)

| Service | Impl | Việc |
|---------|------|------|
| `CategoryService` | `CategoryServiceImpl` | CRUD + filter `groupCode` / `type` / `keyword` |
| `CategoryGroupService` | `CategoryGroupServiceImpl` | **Stub rỗng** — chưa có logic |
| `UxPopupService` | `UxPopupServiceImpl` | Map category UX_POPUP → DTO popup |
| `AssetService` | `AssetServiceImpl` | CRUD, assign/unassign, bảo trì, thanh lý, transfer ticket |
| `DepreciationService` | `DepreciationServiceImpl` | Generate schedule, preview/post GL |

Hỗ trợ: `DepreciationCalculator`, `DepreciationConstants` (tài khoản GL 642/214).  
Error: `DmdcErrorCode`, `DepreciationErrorCode`. Có thể ném `AccountingErrorCode.PERIOD_CLOSED` khi kỳ kế toán đóng.

### 2.3 Initializers

| Class | Seed |
|-------|------|
| `AssetDataInitializer` | Group `LoaiTaiSan` + loại TS + demo assets; migrate legacy `QLTS` → `LoaiTaiSan` |
| `DepreciationDataInitializer` | Schedule demo 36 tháng straight-line cho vài mã TS |
| `SalaryBandDataInitializer` | Group `SalaryBand` + 12 bậc (meta JSON trong `description`) |

---

## 3. Danh mục dùng chung — `/qtht/category`

### 3.1 API

| Method | Path | Action | Mục đích |
|--------|------|--------|----------|
| GET | `/qtht/category` | VIEW | List/filter (`groupCode` hoặc alias `type`, `keyword`, paging) |
| GET | `/qtht/category/{id}` | VIEW | Chi tiết |
| POST | `/qtht/category` | CREATE | Tạo |
| PUT | `/qtht/category/{id}` | UPDATE | Sửa |
| DELETE | `/qtht/category/{id}` | DELETE | Soft-delete |

FE trang quản trị: `/admin/category-management` — cùng API cho mọi group.

### 3.2 `groupCode` quan trọng

| groupCode | Ý nghĩa | Seed chính |
|-----------|---------|------------|
| `ISSUER` | Cơ quan phát hành | `category_data.sql` |
| `SIGNER` | Người ký | `category_data.sql` |
| `ChucDanh` | Chức danh (canonical; legacy `TITLE` migrate lúc boot) | `category_data.sql` |
| `LOCATION` | Địa bàn | `category_data.sql` |
| `INDUSTRY` | Ngành nghề | `category_data.sql` |
| `DonVi` | Đơn vị tính (legacy `UNIT` migrate) | `category_data.sql` |
| `UX_POPUP` | Template popup UX | `category_data.sql` |
| `LoaiTaiSan` | Loại tài sản (dropdown QLTS) | `AssetDataInitializer` |
| `SalaryBand` | Bậc lương | `SalaryBandDataInitializer` |
| `DanhMucSP` | Nhóm SP (product) | Theo doc module / seed riêng |

`cat_group` (số trên `category_group`): 1=Issuer … 7=UX_POPUP (xem comment seed).

**Query mẫu:**

```
GET /qtht/category?groupCode=ISSUER
GET /qtht/category?groupCode=ChucDanh
GET /qtht/category?groupCode=LoaiTaiSan
```

Alias: param `type` = cùng nghĩa `groupCode` (service hỗ trợ).

### 3.3 Entity

**`CategoryGroup` → `category_group`**

| Field | Ý nghĩa |
|-------|---------|
| `code` (PK) | VD `ISSUER`, `ChucDanh` |
| `name` | Nhãn nhóm |
| `catGroup` | Số loại nhóm |
| `isDeleted` | Soft flag |

**`Category` → `categories`**

| Field | Cột | Ý nghĩa |
|-------|-----|---------|
| `code` | `code` | Mã nghiệp vụ |
| `name` / `nameEn` / `shortName` | … | Tên hiển thị |
| `parentCode` | `parent_code` | Cha (nếu cây) |
| **`groupCode`** | **`group_code`** | Phân loại master data |
| `orderIndex` | `order_index` | Sort |
| `description` | `description` | Ghi chú / JSON meta (SalaryBand, UX_POPUP) |
| `active` | `active` | Bật/tắt |
| (+ `BaseEntity`) | | Soft delete / audit |

### 3.4 Phân biệt “Location”

| Khái niệm | Module | Ghi chú |
|-----------|--------|---------|
| `LOCATION` category | dmdc | Địa bàn master data |
| `Asset.location` | dmdc | Chuỗi vị trí vật lý trên tài sản |
| `WarehouseLocation` | `module-warehouse-bom` | Vị trí kho — API `/warehouse/location` |
| `Person.jobTitle` | qtht | Field text — không thay `ChucDanh` category |

---

## 4. UX Popup — `/qtht/ux-popups`

| Method | Path | Mục đích |
|--------|------|----------|
| GET | `/qtht/ux-popups/{eventCode}` | Resolve popup đang active theo mã sự kiện |

Nội dung admin sửa qua: `GET/POST/PUT /qtht/category?groupCode=UX_POPUP`.

---

## 5. Quản lý tài sản (QLTS) — `/qlts/assets`

Base path kép: `/qlts/assets` (canonical) và `/qlts/asset` (compat).

### 5.1 CRUD & vận hành

| Method | Path | Mục đích |
|--------|------|----------|
| GET | `/qlts/assets` | List (keyword, status, categoryCode, assignedPersonId, page, size) |
| GET | `/qlts/assets/{id}` | Chi tiết |
| GET | `/qlts/assets/{id}/history` | Audit `asset_assignment` |
| GET | `/qlts/assets/stats` | KPI |
| POST | `/qlts/assets` | Tạo |
| PUT | `/qlts/assets/{id}` | Sửa (không đổi code/status tùy rule impl) |
| DELETE | `/qlts/assets/{id}` | Soft-delete — **chặn nếu IN_USE** |
| POST | `/{id}/assign` | Gán trực tiếp → `IN_USE` |
| POST | `/{id}/unassign` | Thu hồi → `AVAILABLE` |
| POST | `/{id}/maintenance/start` | → `MAINTENANCE` |
| POST | `/{id}/maintenance/end` | Kết thúc bảo trì (optional cost) |
| POST | `/{id}/dispose` | Thanh lý (terminal) |

### 5.2 Phiếu duyệt chuyển giao

| Method | Path | Status flow |
|--------|------|-------------|
| POST | `/{id}/transfer-requests` | Tạo `PENDING` |
| GET | `/transfer-requests`, `/{reqId}` | List / detail |
| POST | `.../approve` | → `APPROVED` |
| POST | `.../reject` | → `REJECTED` |
| POST | `.../cancel` | → `CANCELLED` |
| POST | `.../handover` | `APPROVED` → `HANDED_OVER` + cập nhật Asset |

`requestType`: `ASSIGN` | `RETURN`. Có thể gắn `workflowInstanceId`.

### 5.3 Entity Asset

**`Asset` → `asset`**

| Field | Ý nghĩa |
|-------|---------|
| `code` | `AS-YYYY-####` |
| `name`, `brand`, `model`, `serialNumber` | Thông tin TS |
| `categoryCode` | Ref `categories.code` group `LoaiTaiSan` |
| `purchaseDate`, `purchasePrice`, `currentValue` | Giá / giá còn lại |
| `warrantyEndDate` | BH |
| **`status`** | `AVAILABLE` \| `IN_USE` \| `MAINTENANCE` \| `BROKEN` \| `DISPOSED` \| `LOST` |
| `location` | Vị trí vật lý (text) |
| `assignedPersonId`, `assignedAt` | Người đang giữ |
| `imageUrl`, `note` | Media / ghi chú |

**`AssetAssignment` → `asset_assignment`** (append-only): `action` = ASSIGN / RETURN / MAINTENANCE_* / DISPOSE / …

**`AssetTransferRequest` → `asset_transfer_request`**: phiếu PENDING → APPROVED → HANDED_OVER | REJECTED | CANCELLED.

### 5.4 Doc nội bộ module

File: `module-dmdc-bom/.../qtbv/doc/list.md` — từ 2026-07 tài sản thật nằm bảng `asset`; loại TS vẫn dùng `categories` `group_code='LoaiTaiSan'`. Không seed mới với `QLTS`.

---

## 6. Khấu hao — `/asset/depreciation`

Alias path: `/qtbv/depreciation` (cùng controller).

| Method | Path | Mục đích |
|--------|------|----------|
| POST | `/schedules/generate` | Sinh lịch (`assetId`, `method?`, `months?`) |
| GET | `/schedules` | List (optional `assetId`) |
| GET | `/preview` | Preview kỳ — không ghi GL |
| POST | `/post` | Post GL kỳ (`year`, `month`) — idempotent `DEP-YYYY-MM` |
| GET | `/postings` | List posting (optional year/month) |

### 6.1 Entity

**`DepreciationSchedule` → `depreciation_schedule`** (unique `asset_id`)

| Field | Ý nghĩa |
|-------|---------|
| `method` | `STRAIGHT_LINE` \| `DECLINING` |
| `startDate`, `months`, `monthlyAmount` | Khung lịch |
| `remainingValue` | Cập nhật sau mỗi post |
| `status` | `ACTIVE` \| `DONE` \| `CANCELLED` |

**`DepreciationPosting` → `depreciation_posting`** (unique year+month)

| Field | Ý nghĩa |
|-------|---------|
| `totalAmount`, `scheduleCount` | Tổng kỳ |
| `journalEntryId` | Ref sổ cái (`module-accounting-bom`) |
| `status` | `POSTED` \| `REVERSED` \| `FAILED` |
| `errorMessage` | Khi FAILED |

---

## 7. Error codes (tóm tắt)

### `DmdcErrorCode`

| Key mẫu | HTTP | Ý nghĩa |
|---------|------|---------|
| `category.code.exist` / `name.exist` | 409 | Trùng danh mục |
| `valid.not.found` | 404 | Không tìm thấy |
| `error.asset.not.found` | 404 | Không có TS |
| `error.asset.code.duplicate` | 409 | Trùng mã |
| `error.asset.in.use.cannot.delete` | 409 | Đang dùng — không xóa |
| `error.asset.not.available` / `not.in.use` / `disposed` | 400 | Sai trạng thái |
| `error.asset.transfer.*` | 4xx | Lỗi phiếu duyệt |

### `DepreciationErrorCode`

| Key | HTTP |
|-----|------|
| `depreciation.asset.not.found` | 404 |
| `depreciation.asset.missing.price` | 400 |
| `depreciation.schedule.exists` | 409 |
| `depreciation.method.invalid` | 400 |
| `depreciation.no.active.schedule` | 400 |

---

## 8. Cross-links

| Module / file | Liên hệ |
|---------------|---------|
| `module-auth-bom` `category_data.sql` | Seed ISSUER/SIGNER/ChucDanh/LOCATION/INDUSTRY/DonVi/UX_POPUP |
| `module-auth-bom` `menu_data.sql` | Menu QLDM_* → `/admin/category-management` |
| [module-qtht-bom.md](./module-qtht-bom.md) | `Person` khi assign TS |
| `module-accounting-bom` | Post GL khấu hao / đóng kỳ |
| [DATABASE_STANDARD.md](../DATABASE_STANDARD.md) | Naming `categories`, soft delete |

```
FE category-management
  → GET /qtht/category?groupCode=ISSUER|SIGNER|ChucDanh|…
FE QLTS
  → GET /qlts/assets?categoryCode=LAPTOP
  → POST /qlts/assets/{id}/assign
Khấu hao
  → POST /asset/depreciation/schedules/generate
  → POST /asset/depreciation/post
```

---

## 9. Checklist

### 9.1 Đọc code lần đầu

- [ ] `CategoryController` + filter `groupCode` — không tìm entity Issuer riêng
- [ ] `category_data.sql` + migrate TITLE→ChucDanh, UNIT→DonVi
- [ ] `AssetController` lifecycle + transfer-requests
- [ ] `AssetDataInitializer` — `LoaiTaiSan` ≠ legacy `QLTS`
- [ ] `DepreciationController` + liên kết accounting
- [ ] Package split: controller `dmdc` / domain `qtbv`

### 9.2 Rule

| ✅ | ❌ |
|----|----|
| Master data Issuer/… qua `categories.group_code` | Tạo bảng/entity riêng không cần thiết |
| Loại TS = `LoaiTaiSan` | Seed mới `group_code='QLTS'` |
| Soft-delete category / asset theo BaseEntity | Xóa cứng TS đang `IN_USE` |
| Post khấu hao idempotent theo kỳ | Post lại cùng kỳ tạo journal trùng |

---

*Cập nhật khi thêm groupCode mới, đổi status Asset, đổi path khấu hao, hoặc implement `CategoryGroupService`.*
