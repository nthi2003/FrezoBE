# Frezo Backend — module-customer-bom (Khách hàng / NCC / Voucher)

> Quản lý master **Customer**, **NCC** (nhà cung cấp rau củ + chứng chỉ), **Voucher** khuyến mãi; bảo mật SĐT (AES-GCM); import/export Excel; sync lead từ FrezoAI scraper.
> Đọc **cùng** [DATABASE_STANDARD.md](../DATABASE_STANDARD.md). Cross-module: CRM (`customer_id`), DMDC (`category_code` / `classification_code`), MinIO (avatar / certificate).

Package gốc: `com.frezo.customer`. Module Maven: `module-customer-bom`.

Context HTTP thường: `/api` → `/api/customer`, `/api/ncc`, `/api/voucher`.

---

## 1. Vai trò & phạm vi

| Hạng mục | Chi tiết |
|----------|----------|
| Customer | CRUD, filter phân trang, Excel import/export, reveal phone (admin), avatar MinIO, AI sync Google Maps |
| NCC | Hồ sơ NCC + diện tích / năng lực / thế mạnh + upload chứng chỉ |
| Voucher | CRUD + validate mã theo đơn hàng |
| Entity có sẵn, **chưa có API** | `CustomerPayment` (`customer_payments`) — chỉ repository |
| Không thuộc module | Lead/Deal CRM; Product/Cart/Order |

```
Customer CRUD (+ encrypt phone via PhoneEncryptionListener)
  ├─ revealPhone → decryptAESGCM + audit log
  ├─ import Excel / export Excel (mask ****last4)
  ├─ uploadAvatar → MinioService
  └─ AiCustomerController.sync → POST localhost:8001/api/v1/scrape → create type=LEAD_AI

NCC CRUD + certificates (ncc_certificates) + upload MinIO
Voucher CRUD + validate(code, orderValue)
```

---

## 2. Class map

### 2.1 Controllers

| Controller | Base path | Vai trò |
|------------|-----------|---------|
| `CustomerController` | `/customer` | CRUD KH, reveal-phone, import/export, avatar |
| `AiCustomerController` | `/customer/ai` | Sync lead từ AI scraper |
| `NCCController` | `/ncc` | CRUD NCC + upload certificate |
| `VoucherController` | `/voucher` | CRUD + validate |

### 2.2 Services

| Class | Vai trò |
|-------|---------|
| `CustomerService` / `CustomerServiceImpl` | CRUD, Excel, AI sync, avatar, revealPhone |
| `NCCService` / `NCCServiceImpl` | CRUD NCC + certificates + upload |
| `VoucherServiceImpl` | CRUD voucher + validate (không interface riêng) |

### 2.3 Mappers / repos

| Class | Vai trò |
|-------|---------|
| `CustomerMapper` / `NCCMapper` | MapStruct DTO ↔ entity |
| `CustomerRepository` | JPA + Specification |
| `NCCRepository` / `NCCCertificateRepository` | NCC + chứng chỉ |
| `VoucherRepository` | `existsByCode`, `findByCode` |
| `CustomerPaymentRepository` | `findByCustomerIdOrderByCreatedDateDesc` — **chưa wire controller** |

### 2.4 Entities → bảng

| Entity | Bảng | Ghi chú |
|--------|------|---------|
| `Customer` | `customers` | `PhoneEncryptable` + `PhoneEncryptionListener` |
| `NCC` | `nccs` | Cùng encrypt phone |
| `NCCCertificate` | `ncc_certificates` | VietGAP / GlobalGAP / Organic / VSATTP… |
| `Voucher` | `vouchers` | PERCENT \| FIXED |
| `CustomerPayment` | `customer_payments` | PENDING/PAID/… — chưa API |

### 2.5 Error codes — `CustomerErrorCode`

| Nhóm | Key |
|------|-----|
| Customer | `CUSTOMER_NOT_FOUND`, `CUSTOMER_CODE_EXISTS`, `CUSTOMER_IMPORT_FAILED`, `CUSTOMER_EXPORT_FAILED`, `AI_CONNECTION_FAILED` |
| Voucher | `VOUCHER_NOT_FOUND`, `VOUCHER_CODE_EXISTS`, `VOUCHER_INACTIVE`, `VOUCHER_EXPIRED`, `VOUCHER_NOT_STARTED`, `VOUCHER_MIN_ORDER_NOT_MET`, `VOUCHER_MAX_USAGE` |
| NCC | `NCC_NOT_FOUND`, `NCC_CODE_EXISTS` |

---

## 3. API map

### 3.1 Customer — `/customer`

| Method | Path | Action | Ghi chú |
|--------|------|--------|---------|
| `GET` | `/customer` | VIEW | `CustomerFilterRequest` (paging + filter) |
| `GET` | `/customer/{id}` | VIEW | |
| `POST` | `/customer` | CREATE | Body `CustomerRequest` |
| `PUT` | `/customer/{id}` | UPDATE | |
| `DELETE` | `/customer/{id}` | DELETE | Soft-delete (qua service) |
| `GET` | `/customer/{id}/reveal-phone` | VIEW | Thêm `@PreAuthorize` ADMIN hoặc `CUSTOMER_REVEAL_PHONE` |
| `POST` | `/customer/import` | UPDATE | Multipart Excel |
| `GET` | `/customer/export` | VIEW | Stream `.xlsx` (SĐT mask `****` + last4) |
| `POST` | `/customer/{id}/avatar` | CREATE | Multipart → MinIO `customers/{code}/avatar_*` |

### 3.2 AI sync — `/customer/ai`

| Method | Path | Action | Params |
|--------|------|--------|--------|
| `POST` | `/customer/ai/sync` | UPDATE | `keyword`, `city`, `ward?`, `limit` (default 10) |

Upstream: `http://localhost:8001/api/v1/scrape`. Response status `success` → tạo KH `type=LEAD_AI`, `status=POTENTIAL`, `categoryCode=KHTN_AI`. Bỏ SĐT rỗng/`N/A`. Lỗi kết nối → `AI_CONNECTION_FAILED`.

### 3.3 NCC — `/ncc`

| Method | Path | Action | Ghi chú |
|--------|------|--------|---------|
| `GET` | `/ncc/all` | VIEW | `NCCFilterRequest` |
| `GET` | `/ncc/{id}` | VIEW | Kèm certificates trong response |
| `POST` | `/ncc` | CREATE | Body `NCCRequest` (+ certificates inline) |
| `PUT` | `/ncc/{id}` | UPDATE | |
| `DELETE` | `/ncc/{id}` | DELETE | |
| `POST` | `/ncc/upload-certificate` | CREATE | Params `nccCode` + `file` → MinIO |

Response list dùng `ApiResponse.success(...)` (khác customer dùng `ok` ở nhiều chỗ — cùng envelope).

### 3.4 Voucher — `/voucher`

| Method | Path | Action | Ghi chú |
|--------|------|--------|---------|
| `GET` | `/voucher` | VIEW | `keyword`, `status`, `discountType`, paging |
| `POST` | `/voucher` | CREATE | Body entity `Voucher`; auto-code `VC…` nếu trống |
| `PUT` | `/voucher/{id}` | UPDATE | Không đổi `code` |
| `DELETE` | `/voucher/{id}` | DELETE | **Hard delete** (`deleteById`) |
| `GET` | `/voucher/validate` | VIEW | `code` + `orderValue` → trả voucher nếu hợp lệ |

---

## 4. Entity fields chính

### 4.1 Customer (`customers`)

| Field | Ý nghĩa |
|-------|---------|
| `name`, `code` | `code` unique; generate nếu trống (`SecureCodeGenerator`) |
| `phone` | Ephemeral plaintext (listener encrypt) |
| `phoneEncrypted` / `phoneHash` / `phoneLast4` | Bảo mật + tìm kiếm hash / mask UI |
| `email`, `address`, `taxCode` | |
| `type` | VD `INDIVIDUAL`, `COMPANY`, `LEAD_AI` |
| `status` | VD `ACTIVE`, `INACTIVE`, `POTENTIAL` |
| `categoryCode` | Ref danh mục group `KHTN` (DMDC) |
| `note`, `avatarUrl` | |

### 4.2 NCC (`nccs`)

| Field | Ý nghĩa |
|-------|---------|
| `name`, `code`, `representative` | |
| Phone encrypt giống Customer | |
| `classificationCode` | groupCode `PhanLoaiNCC` (DMDC) |
| `growingArea` | Diện tích vùng trồng |
| `maxCapacity` | Năng lực cung ứng / ngày |
| `strengths` | Sản phẩm thế mạnh |

### 4.3 NCCCertificate (`ncc_certificates`)

| Field | Ý nghĩa |
|-------|---------|
| `nccId`, `certificateType`, `fileUrl`, `expiryDate` | |

### 4.4 Voucher (`vouchers`)

| Field | Ý nghĩa |
|-------|---------|
| `code`, `name` | unique code |
| `discountType` | `PERCENT` \| `FIXED` |
| `discountValue`, `minOrderValue` | |
| `maxUsage` / `usedCount` | null max = không giới hạn |
| `startDate` / `endDate` | |
| `status` | `ACTIVE` \| `INACTIVE` \| `EXPIRED` |

### 4.5 CustomerPayment (`customer_payments`) — chưa expose API

| Field | Ý nghĩa |
|-------|---------|
| `customerId`, `invoiceCode`, `amount` | |
| `status` | PENDING \| PAID \| PARTIAL \| OVERDUE \| CANCELLED |
| `dueDate`, `paidDate`, `note` | |

---

## 5. Luồng chính

### 5.1 Bảo mật SĐT

```
Customer / NCC implements PhoneEncryptable
  → PhoneEncryptionListener (@EntityListeners)
       → persist: encrypt → phoneEncrypted, phoneHash, phoneLast4; clear plaintext
GET /customer/{id}/reveal-phone
  → CryptoUtils.decryptAESGCM + log [AUDIT] username + customerId
```

| Rule | |
|------|--|
| ✅ Export Excel chỉ `****` + last4 — không plaintext | |
| ✅ Reveal cần ADMIN hoặc authority `CUSTOMER_REVEAL_PHONE` | |
| ❌ Không trả `phoneEncrypted` ra API thường | |

### 5.2 Import Excel Customer

Cột (row ≥ 1): `name`, `phone`, `email`, `address`, `taxCode`, `type` → gọi `create` từng dòng. Lỗi → `CUSTOMER_IMPORT_FAILED`.

### 5.3 Validate voucher

```
findByCode → ACTIVE?
→ today trong [startDate, endDate]
→ orderValue >= minOrderValue
→ usedCount < maxUsage (nếu có)
→ return Voucher (caller tự tính giảm / tăng usedCount nếu apply)
```

| Rule | |
|------|--|
| ⚠️ `validate` **không** tăng `usedCount` — chỉ kiểm tra | |
| ❌ Soft-delete voucher — hiện hard delete | |

### 5.4 AI sync

| Bước | Việc |
|------|------|
| 1 | POST FrezoAI scrape |
| 2 | Skip phone empty / `N/A` |
| 3 | `create` với address suffix `(Source: Google Maps)` |
| 4 | Trùng / lỗi create → warn skip, không fail cả batch |

---

## 6. Dependencies (Maven)

| Artifact | Lý do |
|----------|-------|
| `module-common` | BaseEntity, ApiResponse, CheckPermission, CryptoUtils, MinioService, Spec helpers |
| `module-dmdc-bom` | Danh mục `categoryCode` / phân loại NCC |
| `spring-boot-starter-data-jpa` / `web` | Persist + REST |
| MapStruct | CustomerMapper / NCCMapper |
| Apache POI (transitive / parent) | Import/export Excel |

---

## 7. Cross-links

| Module / file | Liên hệ |
|---------------|---------|
| [module-crm-bom.md](./module-crm-bom.md) | Deal/Invoice/Meeting.customerId; Lead convert gắn KH |
| [module-product-bom.md](./module-product-bom.md) | Cart/Order theo `customerId`; Batch.supplierId ≈ NCC |
| `module-dmdc-bom` | Category group KHTN / PhanLoaiNCC |
| `module-common` | `PhoneEncryptionListener`, `MinioService`, `SecureCodeGenerator` |
| [DATABASE_STANDARD.md](../DATABASE_STANDARD.md) | Naming `customers`, `nccs`, soft-delete |

```
[FE ERP]
  → /api/customer | /customer/ai/sync | /ncc | /voucher
[FrezoAI :8001]
  ← scrape → Customer LEAD_AI
[MinIO]
  ← avatar / certificate files
[CRM / Product]
  ← customerId FK logic (string id)
```

---

## 8. Checklist

### 8.1 Đọc code lần đầu

- [ ] `CustomerController` + `CustomerServiceImpl` (reveal / Excel / AI / avatar)
- [ ] `Customer` entity + `PhoneEncryptable` listener
- [ ] `NCCController` + `NCCServiceImpl` + `NCCCertificate`
- [ ] `VoucherController` + `VoucherServiceImpl.validate`
- [ ] `CustomerErrorCode` đầy đủ
- [ ] Xác nhận `CustomerPayment` **chưa** có controller

### 8.2 Rule bắt buộc

| ✅ | ❌ |
|----|----|
| Encrypt phone khi persist | Lưu plaintext lâu dài trên cột `phone` |
| Reveal có PreAuthorize + audit log | Reveal cho mọi user VIEW |
| Export mask SĐT | Export full phone |
| Voucher validate đủ status/date/min/usage | Apply khi INACTIVE |
| Phân biệt soft-delete Customer vs hard-delete Voucher | Giả định mọi delete đều soft |

### 8.3 Kiểm thử thủ công gợi ý

- [ ] CRUD customer → list chỉ thấy last4
- [ ] Admin reveal-phone → plaintext + log AUDIT
- [ ] Import Excel vài dòng → tạo mới
- [ ] Sync AI (FrezoAI chạy) → KH type LEAD_AI
- [ ] Voucher ACTIVE hợp lệ / hết hạn / dưới min order
- [ ] Upload chứng chỉ NCC theo `nccCode`

---

*Cập nhật khi expose API `CustomerPayment`, đổi URL FrezoAI, hoặc đổi policy encrypt phone.*
