# Frezo Backend — module-warehouse-bom (Kho vận)

> Module kho: master data, tồn kho, GRN/GIN, lô/FEFO, chuyển/điều chỉnh, kiểm kê, hao hụt, PR/PO, cảnh báo & reorder, báo cáo.
> Đọc **cùng** [DATABASE_STANDARD.md](../DATABASE_STANDARD.md). Cross-module: product, approval, notification, MinIO — mục 12. Kế toán (chưa bridge GL) — [module-accounting-bom.md](./module-accounting-bom.md).

Package gốc: `com.frezo.warehouse`. Module Maven: `module-warehouse-bom`.  
Base API: `/warehouse/*`. Dependencies: `module-common`, `module-auth-bom`, `module-product-bom`, `module-approval-bom`.

---

## 1. Vai trò module

| Nhóm | Mô tả |
|------|-------|
| Master data | Kho → khu vực → vị trí (`warehouses` / `warehouse_zones` / `warehouse_locations`) |
| Tồn kho | Số dư (`stock_balances`), sổ cái (`stock_ledger`), giao dịch tổng (`stock_transactions`) |
| Nhập / Xuất | GRN (phiếu nhập), GIN (phiếu xuất) — workflow duyệt + confirm cập nhật tồn |
| Lô / FEFO | `stock_batch`, gợi ý First-Expired-First-Out, cảnh báo cận hạn |
| Vận hành | Chuyển kho, điều chỉnh, kiểm kê, hao hụt |
| Mua hàng | PR từ cảnh báo, PO từ PR |
| Cảnh báo & tái nhập | `reorder_rule`, `stock_alert`, job `STOCK_ALERT_SCAN` |
| Báo cáo | NXT, tồn thấp, export Excel |

**Mô hình tồn:** mỗi dòng `stock_balances` định danh bởi `(product_id, warehouse_id, location_id, batch_id)`. Confirm/post ghi `stock_ledger` và (GRN/GIN/Transfer) thêm `stock_transactions`.

```
Master: Warehouse → Zone → Location
Inbound:  PR → PO → GRN.confirm → StockBatch + Balance + Ledger + Transaction(RECEIPT)
Outbound: GIN.confirm (FEFO) → trừ Batch + Balance + Ledger + Transaction(ISSUE)
Ops:     Transfer / Adjustment / Shrinkage / StockTake
Alerts:  ReorderRule + StockAlertJob → StockAlert → PR from alerts
```

---

## 2. Class map

### 2.1 Controllers (18)

| Controller | Path prefix | Vai trò |
|------------|-------------|---------|
| `WarehouseController` | `/warehouse` | CRUD kho |
| `WarehouseZoneController` | `/warehouse/zone` | CRUD khu vực |
| `WarehouseLocationController` | `/warehouse/location` | CRUD vị trí + barcode |
| `StockBalanceController` | `/warehouse/stock` | Tra cứu tồn, stats, alerts, export |
| `StockReportController` | `/warehouse/report` | NXT, low-stock, export |
| `GoodsReceiptNoteController` | `/warehouse/grn` | GRN lifecycle + print + price-history |
| `GrnAttachmentController` | `/warehouse/grn/{grnId}/attachments` | MinIO đính kèm GRN |
| `GoodsIssueNoteController` | `/warehouse/gin` | GIN lifecycle + FEFO + batch confirm |
| `GinAttachmentController` | `/warehouse/gin/{ginId}/attachments` | MinIO đính kèm GIN |
| `StockTransferController` | `/warehouse/transfer` | Chuyển kho |
| `StockAdjustmentController` | `/warehouse/adjustment` | Điều chỉnh tồn |
| `StockTakeController` | `/warehouse/stock-takes` | Kiểm kê |
| `StockShrinkageController` | `/warehouse/shrinkage` | Hao hụt |
| `StockBatchController` | `/warehouse/batches` | Tra cứu lô |
| `ReorderController` | `/warehouse` | reorder-rules, stock-alerts, warehouses options |
| `PurchaseRequestController` | `/warehouse/purchase-requests` | PR |
| `PurchaseOrderController` | `/warehouse/purchase-orders` | PO |

### 2.2 Services chính

| Interface | Impl | Vai trò |
|-----------|------|---------|
| `WarehouseService` | `WarehouseServiceImpl` | CRUD kho |
| `WarehouseZoneService` | `WarehouseZoneServiceImpl` | CRUD zone |
| `WarehouseLocationService` | `WarehouseLocationServiceImpl` | CRUD location |
| `StockBalanceService` | `StockBalanceServiceImpl` | Tồn, stats, export |
| `StockReportService` | `StockReportServiceImpl` | Báo cáo NXT |
| `GoodsReceiptNoteService` | `GoodsReceiptNoteServiceImpl` | GRN + confirm |
| `GoodsIssueNoteService` | `GoodsIssueNoteServiceImpl` | GIN + FEFO confirm |
| `StockTransferService` | `StockTransferServiceImpl` | Chuyển kho |
| `StockAdjustmentService` | `StockAdjustmentServiceImpl` | Điều chỉnh |
| `StockTakeService` | `StockTakeServiceImpl` | Kiểm kê |
| `StockShrinkageService` | `StockShrinkageServiceImpl` | Hao hụt |
| `StockBatchService` | `StockBatchServiceImpl` | Lô, FEFO, sinh batch code |
| `ReorderService` | `ReorderServiceImpl` | Reorder + LOW_STOCK scan |
| `ExpiryAlertService` | `ExpiryAlertServiceImpl` | EXPIRY_SOON scan |
| `PurchaseRequestService` | `PurchaseRequestServiceImpl` | PR + approval |
| `PurchaseOrderService` | `PurchaseOrderServiceImpl` | PO từ PR |
| `GrnAttachmentService` / `GinAttachmentService` | `*Impl` | Upload MinIO |
| `StockAlertNotifier` | — | Notification chuông |
| `DocumentPrintService` | — | In HTML / export Excel |

### 2.3 Jobs / events / listeners

| Class | Vai trò |
|-------|---------|
| `StockAlertJob` | `SchedulableJob` code `STOCK_ALERT_SCAN`, cron mặc định `0 0 6 * * *` |
| `GrnConfirmedEvent` | Publish sau GRN confirm — **chưa có listener kế toán** |
| `PurchaseRequestApprovalListener` | `ApprovalDecidedEvent` → alert RESOLVED / gỡ link |

### 2.4 Package layout

```
com.frezo.warehouse/
├── common/WarehouseErrorCode.java
├── controller/          # 18 REST
├── dto/request|response/
├── entity/              # 27 JPA
├── event/GrnConfirmedEvent.java
├── job/StockAlertJob.java
├── listener/PurchaseRequestApprovalListener.java
├── mapper/              # MapStruct GRN, GIN, Transfer
├── repository/
└── service/ + impl/
```

---

## 3. API endpoints

> Mọi endpoint có `@CheckPermission`. Response: `ApiResponse` / `FePage`. Context thường `/api`.

### 3.1 Master — kho / zone / location

| Method | Path | Mục đích |
|--------|------|----------|
| GET | `/warehouse` | Danh sách kho |
| GET | `/warehouse/search` | Tìm kiếm phân trang (`keyword`, `status`) |
| GET | `/warehouse/{id}`, `/warehouse/code/{code}` | Chi tiết |
| POST / PUT / DELETE | `/warehouse`, `/{id}` | CRUD kho |
| GET | `/warehouse/zone/by-warehouse/{warehouseId}` | Zone theo kho |
| CRUD | `/warehouse/zone`, `/{id}` | Zone |
| GET | `/warehouse/location/by-zone/{zoneId}`, `/by-warehouse/{warehouseId}`, `/barcode/{barcode}` | Tra vị trí |
| CRUD | `/warehouse/location`, `/{id}` | Location |

### 3.2 Tồn & báo cáo

| Method | Path | Mục đích |
|--------|------|----------|
| GET | `/warehouse/stock` | Lọc tồn (`warehouseId`, `productId`, `keyword`) |
| GET | `/warehouse/stock/{id}` | Chi tiết 1 dòng |
| GET | `/warehouse/stock/alerts` | Cảnh báo theo `Product.warningThreshold` |
| GET | `/warehouse/stock/stats` | Dashboard kho |
| GET | `/warehouse/stock/export` | Excel tồn |
| GET | `/warehouse/report/stock-movement` | NXT (`from`, `to`, `productId`, `warehouseId`) |
| GET | `/warehouse/report/low-stock` | Tồn thấp |
| GET | `/warehouse/report/stock-movement/export` | Excel NXT |

### 3.3 GRN / GIN

| Method | Path | Mục đích |
|--------|------|----------|
| POST | `/warehouse/grn` | Tạo GRN |
| PUT | `/warehouse/grn/{id}` | Sửa invoice/note (chưa CONFIRMED) |
| POST | `/warehouse/grn/{id}/submit` | → PENDING_APPROVAL |
| POST | `/warehouse/grn/{id}/approve` | → APPROVED |
| POST | `/warehouse/grn/{id}/confirm` | Confirm nhập → cập nhật tồn + lô |
| POST | `/warehouse/grn/{id}/cancel` | Hủy (`?reason`) |
| GET | `/warehouse/grn/product/{productId}/price-history` | Lịch sử giá nhập |
| GET | `/warehouse/grn/{id}`, `/code/{grnCode}`, list, DELETE, `/{id}/print` | Tra cứu / xóa / in |
| CRUD attach | `/warehouse/grn/{grnId}/attachments` | Upload / list / delete MinIO |
| GET | `/warehouse/gin/fefo-suggest` | Gợi ý lô FEFO (`warehouseId`, `productId`, `qty`) |
| POST | `/warehouse/gin` | Tạo GIN |
| POST | `/warehouse/gin/{id}/submit\|approve\|confirm\|cancel` | Workflow |
| POST | `/warehouse/gin/batch-confirm`, `/batch-cancel` | Hàng loạt |
| GET | `/warehouse/gin/{id}/print`, `/{id}/export` | In HTML / Excel |
| CRUD attach | `/warehouse/gin/{ginId}/attachments` | MinIO |

### 3.4 Transfer / Adjustment / StockTake / Shrinkage / Batch

| Method | Path | Mục đích |
|--------|------|----------|
| POST | `/warehouse/transfer` | Tạo phiếu chuyển |
| POST | `/warehouse/transfer/{id}/confirm\|cancel` | Confirm / hủy |
| GET / DELETE / print | `/warehouse/transfer/...` | Tra cứu, xóa DRAFT, in |
| POST | `/warehouse/adjustment` | Tạo điều chỉnh |
| POST | `/warehouse/adjustment/{id}/confirm\|cancel` | Confirm / hủy |
| GET | `/warehouse/stock-takes` | List kiểm kê |
| POST | `/warehouse/stock-takes` | Tạo |
| POST | `/warehouse/stock-takes/{id}/start` | → IN_PROGRESS |
| POST | `/warehouse/stock-takes/{id}/submit-counted` | Nhập số đếm |
| POST | `/warehouse/stock-takes/{id}/post-variance` | Post chênh lệch (**stub**) |
| POST | `/warehouse/shrinkage` | Tạo hao hụt |
| POST | `/warehouse/shrinkage/{id}/confirm\|cancel` | Confirm / hủy |
| GET | `/warehouse/batches`, `/{id}` | Tra cứu lô |

### 3.5 Reorder / Alerts / PR / PO

| Method | Path | Mục đích |
|--------|------|----------|
| GET | `/warehouse/warehouses` | Option combobox kho |
| GET/POST/PUT/DELETE | `/warehouse/reorder-rules` | Quy tắc tái nhập |
| POST | `/warehouse/reorder-rules/import-excel` | Import (**stub MVP**) |
| GET | `/warehouse/stock-alerts` | List cảnh báo |
| POST | `/warehouse/stock-alerts/scan` | Scan thủ công |
| POST | `/warehouse/stock-alerts/{id}/dismiss` | Bỏ qua |
| POST | `/warehouse/purchase-requests/from-alerts` | Sinh PR từ alert |
| CRUD + submit | `/warehouse/purchase-requests` | PR thủ công / submit duyệt |
| CRUD | `/warehouse/purchase-orders` | PO |
| POST | `/warehouse/purchase-orders/from-pr/{prId}` | PO từ PR APPROVED (idempotent) |
| POST | `/warehouse/purchase-orders/{id}/confirm-receive` | Nhận hàng (**stub** — không thay GRN) |

---

## 4. Master data — kho / zone / location

| Bước | Hành động | Service | Ghi chú |
|------|-----------|---------|---------|
| 1 | Tạo `Warehouse` (code unique) | `WarehouseServiceImpl.create` | type: `MAIN` / `TRANSIT` / `RETURNS` / `VIRTUAL`; status: `ACTIVE` / `INACTIVE` / `CLOSED` |
| 2 | Tạo `WarehouseZone` | `WarehouseZoneServiceImpl` | type: `STORAGE` / `PICKING` / `STAGING` / `QUARANTINE` |
| 3 | Tạo `WarehouseLocation` | `WarehouseLocationServiceImpl` | aisle / rack / level / bin + barcode |
| 4 | Tra cứu | Controllers GET | Hierarchy: Warehouse → Zone → Location |

**Rule:**
- ✅ Code kho unique trong hệ thống
- ❌ Không confirm GRN khi dòng thiếu `location_id` (qty > 0)

---

## 5. Tồn kho — balance / ledger / transaction

| Thành phần | Bảng | Vai trò |
|------------|------|---------|
| Số dư | `stock_balances` | `quantity_on_hand`, `quantity_reserved`, `quantity_available` theo (SP, kho, vị trí, lô) |
| Sổ cái | `stock_ledger` | Mỗi dòng IN / OUT / ADJUSTMENT: qty, unit_cost, reference_type/id |
| Giao dịch tổng | `stock_transactions` | Header RECEIPT / ISSUE / TRANSFER khi confirm GRN/GIN/Transfer |

Cập nhật tồn nằm **trong từng** `*ServiceImpl.confirm` (`updateStockBalance` / `deductStockBalance`) — **không** có stock engine trung tâm.

| Rule | |
|------|--|
| ✅ Mọi confirm nghiệp vụ phải ghi ledger | |
| ❌ Không sửa trực tiếp `stock_balances` ngoài luồng confirm | |
| ⚠️ Không `@Version` / pessimistic lock — race khi 2 confirm cùng batch | |

---

## 6. GRN — phiếu nhập kho

### 6.1 State machine

```
DRAFT ──submit──► PENDING_APPROVAL ──approve──► APPROVED
  │                      │                         │
  └──────────────────────┴────confirm──────────────┘
                              │
                              ▼
                          CONFIRMED (terminal)

DRAFT / PENDING / APPROVED ──cancel──► CANCELLED
```

Confirm được phép từ **DRAFT hoặc APPROVED** (có thể bỏ qua submit/approve).

### 6.2 Confirm — `GoodsReceiptNoteServiceImpl.confirm`

| # | Hành động |
|---|-----------|
| 1 | Validate status DRAFT hoặc APPROVED |
| 2 | Gắn NCC hoặc PO → bắt buộc `invoice_no` (`GRN_INVOICE_REQUIRED`) |
| 3 | Mỗi dòng qty > 0 → bắt buộc `location_id` |
| 4 | Tạo `StockBatch` nếu chưa có (batch_code, expiry từ request hoặc `computeExpiryDate`) |
| 5 | Ghi `stock_ledger` type IN, reference GRN |
| 6 | Cộng `stock_balances` |
| 7 | Tạo `stock_transactions` type RECEIPT |
| 8 | Publish `GrnConfirmedEvent` |

### 6.3 Attachments & price history

| Hạng mục | Chi tiết |
|----------|----------|
| MinIO path | `grn/{grnId}/{timestamp}_{filename}` |
| Price history | GET `/warehouse/grn/product/{productId}/price-history` — `unit_cost` từ GRN items (bỏ CANCELLED) |

**Rule:**
- ✅ CONFIRMED → không cancel / delete
- ✅ Soft-edit HĐ NCC qua PUT trước confirm
- ❌ Dùng `PO.confirm-receive` thay GRN để nhập chuẩn (có lô/vị trí)

---

## 7. GIN — phiếu xuất kho

### 7.1 Workflow

| Bước | Status | API |
|------|--------|-----|
| Tạo | DRAFT | POST `/warehouse/gin` |
| Submit | PENDING_APPROVAL | POST `/{id}/submit` |
| Approve | APPROVED | POST `/{id}/approve` |
| Confirm | CONFIRMED | POST `/{id}/confirm` |
| Batch confirm / cancel | — | POST `/batch-confirm`, `/batch-cancel` |

**issue_type:** `SALES`, `INTERNAL_TRANSFER`, `DAMAGE_RETURN`, `ADJUSTMENT`.

### 7.2 FEFO

```
GET /warehouse/gin/fefo-suggest
  → StockBatchServiceImpl.suggestFefo
       → StockBatchRepository.findAvailableForFefo
            (expiry_date ASC, received ASC) → phân bổ qty
```

**Confirm:** trừ `stock_batch.qty_on_hand` (→ `DEPLETED` nếu = 0), trừ balance, ledger OUT, transaction ISSUE.

**Rule:**
- ✅ Ưu tiên gợi ý FEFO trước khi confirm xuất
- ❌ Xuất vượt tồn lô → `BATCH_INSUFFICIENT` / `STOCK_BALANCE_INSUFFICIENT`

---

## 8. Transfer / Adjustment

### 8.1 Transfer

| Bước | Status | Logic |
|------|--------|-------|
| Tạo | DRAFT | Validate from ≠ to warehouse |
| Confirm | CONFIRMED | OUT kho nguồn + IN kho đích (2 ledger / dòng) |
| Cancel | CANCELLED | Chỉ khi chưa CONFIRMED |

⚠️ Transfer confirm **không** cập nhật `stock_batch.qty_on_hand` khi chuyển giữa kho — chỉ balance.

### 8.2 Adjustment

| Bước | Status | Logic |
|------|--------|-------|
| Tạo | DRAFT | `diff = actual − expected` |
| Confirm | CONFIRMED | Ledger IN/OUT theo `diff_qty` |

---

## 9. Stock batch / FEFO / expiry

| Khái niệm | Chi tiết |
|-----------|----------|
| Sinh lô | GRN confirm hoặc `batch_code` trong confirm request |
| Format code | `LOT-{productCode}-{supplierCode}-{yyyyMMdd}-{seq}` |
| Shelf life | Rau (`LSP_VEG`): 3 ngày; củ: 7 ngày; hoặc `Product.expiryAlertDays` |
| Status | `ACTIVE` / `DEPLETED` / `EXPIRED` |
| API tra cứu | GET `/warehouse/batches` (filter warehouse, product, status, expiry) |

Migration: `V202607291000__warehouse_sme_batch_shrinkage.sql` — CREATE `stock_batch`, bảng shrinkage, ALTER `stock_alert`.

---

## 10. Cảnh báo & reorder

### 10.1 ReorderRule

Unique `(product_id, warehouse_id)` — `min_qty`, `max_qty`, `reorder_qty`, `preferred_supplier_id`.

⚠️ `preferred_supplier_id` có trên entity/DB nhưng **API create/update chưa expose** đầy đủ — dùng khi sinh PR từ alert (đọc rule có sẵn).

### 10.2 Scan LOW_STOCK — `ReorderServiceImpl.scanAndRaiseAlerts`

| Điều kiện | Hành động |
|-----------|-----------|
| `sum(quantity_available) < min_qty` | Tạo `StockAlert` OPEN; severity CRITICAL nếu qty = 0 |
| Idempotency | Key `{productId}\|{warehouseId}\|{yyyyMMdd}` |
| Notify | `StockAlertNotifier.notifyAlert` (cap ~30/user/ngày, menu `WH_ALERTS`) |

### 10.3 Scan EXPIRY — `ExpiryAlertServiceImpl`

| Điều kiện | alert_type |
|-----------|------------|
| `days_to_expiry <= alertDays` | `EXPIRY_SOON` |
| Default alertDays | Product.expiryAlertDays hoặc 1 (rau) / 3 (củ) |

### 10.4 Job

| Hạng mục | Giá trị |
|----------|---------|
| Code | `STOCK_ALERT_SCAN` |
| Cron mặc định | `0 0 6 * * *` (06:00) |
| Migration seed job | `V202608051610__system_job.sql` |
| Hành vi | `scanAndRaiseAlerts` + `scanAndRaiseExpiryAlerts` + notify safety-net |

### 10.5 StockAlert state

```
OPEN ──dismiss──► DISMISSED
OPEN ──PR approved──► RESOLVED   (PurchaseRequestApprovalListener)
```

**alert_type:** `LOW_STOCK` | `EXPIRY_SOON`. **severity:** `CRITICAL` / `WARNING` / `INFO`.

---

## 11. PR / PO

### 11.1 PR từ alerts

`PurchaseRequestServiceImpl.createFromAlerts`:

| Bước | Logic |
|------|-------|
| 1 | Nhóm alert theo `(preferred_supplier_id, warehouse_id)` |
| 2 | Sinh PR DRAFT, lines gắn `stock_alert_id` |
| 3 | `alert.purchase_request_id = PR id` |
| 4 | Qty = reorder_qty hoặc max−min hoặc min−current |

### 11.2 PR submit

| Config | Hành vi |
|--------|---------|
| `warehouse.pr.approval.required=true` (default) | DRAFT → PENDING, `ApprovalCreator` flow `PURCHASE_REQUEST` |
| `=false` | Bypass → APPROVED ngay |

```
DRAFT ──submit──► PENDING ──approval──► APPROVED | REJECTED
DRAFT ──submit (bypass)──► APPROVED
```

### 11.3 PO

```
DRAFT ──(from-pr)──► CONFIRMED ──confirm-receive──► RECEIVED
```

| API | Logic |
|-----|-------|
| POST `/from-pr/{prId}` | Idempotent 1 PR → 1 PO (`findByPrIdAndIsDeletedFalse`) |
| POST `/{id}/confirm-receive` | **Stub:** tăng tồn `(product, warehouse, null, null)` — không qua GRN/lô |

**Luồng nhập chuẩn:** PO → **GRN.confirm** (có lô + vị trí) — không dựa `confirm-receive`.

---

## 12. Stocktake & Shrinkage

### 12.1 StockTake

```
DRAFT ──start──► IN_PROGRESS ──submit-counted──► SUBMITTED ──post-variance──► POSTED
```

| Bước | Logic |
|------|-------|
| Tạo | Lines lấy `system_qty` = sum balance |
| Submit counted | `variance = counted − system` |
| Post variance | **STUB:** chỉ log / đổi status — **chưa** tạo `StockAdjustment` |

### 12.2 Shrinkage

| Bước | Status | Logic |
|------|--------|-------|
| Tạo | DRAFT | Lines: `batch_id`, reason `SHRINK` / `DAMAGE` / `EXPIRED`, qty |
| Confirm | CONFIRMED | Trừ batch + balance + ledger OUT ref SHRINKAGE |
| Cancel | CANCELLED | Chỉ DRAFT |

---

## 13. Báo cáo

| Report | API | Nguồn |
|--------|-----|-------|
| NXT | GET `/warehouse/report/stock-movement` | `stock_ledger` group by product |
| Low stock | GET `/warehouse/report/low-stock` | balance vs `products.warning_threshold` |
| Export NXT | GET `/stock-movement/export` | Apache POI Excel |
| Export tồn | GET `/warehouse/stock/export` | Excel |

---

## 14. Bảng DB

> Schema core phần lớn từ JPA / seed. Flyway mới: `V202607291000__warehouse_sme_batch_shrinkage.sql`, `V202608051610__system_job.sql`. Audit: `BaseEntity` (`id`, `created_*`, `updated_*`, `is_deleted`, `deleted_*`).

### 14.1 Master & tồn

| Bảng | Entity | Cột / quan hệ chính |
|------|--------|---------------------|
| `warehouses` | `Warehouse` | code UK, type, status, is_default |
| `warehouse_zones` | `WarehouseZone` | warehouse_id, code, type |
| `warehouse_locations` | `WarehouseLocation` | zone_id, aisle/rack/level/bin, barcode |
| `stock_balances` | `StockBalance` | UK (product, warehouse, location, batch); qty_on_hand/reserved/available |
| `stock_ledger` | `StockLedger` | transaction_type, quantity, unit_cost, reference_type/id |
| `stock_transactions` | `StockTransaction` | transaction_code UK, type RECEIPT/ISSUE/TRANSFER |

### 14.2 GRN / GIN

| Bảng | Status / type |
|------|---------------|
| `goods_receipt_notes` + `_items` | DRAFT, PENDING_APPROVAL, APPROVED, CONFIRMED, CANCELLED |
| `grn_attachments` | file_url MinIO |
| `goods_issue_notes` + `_items` | Status tương GRN; issue_type |
| `gin_attachments` | file_url MinIO |

### 14.3 Vận hành & mua hàng

| Bảng | Status |
|------|--------|
| `stock_transfers` + `_items` | DRAFT, CONFIRMED, CANCELLED |
| `stock_adjustments` + `_items` | DRAFT, CONFIRMED, CANCELLED |
| `stock_take` + `stock_take_line` | DRAFT, IN_PROGRESS, SUBMITTED, POSTED, CANCELLED |
| `stock_batch` | ACTIVE, DEPLETED, EXPIRED |
| `stock_shrinkage` + `_line` | DRAFT, CONFIRMED, CANCELLED |
| `reorder_rule` | UK (product_id, warehouse_id) |
| `stock_alert` | OPEN / DISMISSED / RESOLVED; LOW_STOCK / EXPIRY_SOON |
| `purchase_request` + `_line` | DRAFT, PENDING, APPROVED, REJECTED, CANCELLED |
| `purchase_order` + `_line` | DRAFT, CONFIRMED, RECEIVED, CANCELLED |

### 14.4 Soft delete

| Soft-delete | Hard delete |
|-------------|-------------|
| ReorderRule, PR/PO (+ lines), StockTake, Shrinkage, StockBatch (query filter) | GRN/GIN/Transfer/Adjustment (chưa confirm), Warehouse delete trực tiếp |

Mã phiếu auto (`SecureCodeGenerator`): prefix `GRN`, `GIN`, `TF`, `ADJ`, `SHR`, `ST`.

---

## 15. Error codes — `WarehouseErrorCode`

| Nhóm | Key (ví dụ) | HTTP |
|------|-------------|------|
| Kho | `warehouse.not.found`, `warehouse.code.exists`, zone/location not found | 404 / 400 |
| Tồn | `stock.balance.not.found`, `stock.balance.insufficient`, `stock.export.failed` | 404 / 400 / 500 |
| Transfer | `stock.transfer.*` (same warehouse, already confirmed, cannot cancel/delete) | 400 / 404 |
| Adjustment | `stock.adjustment.*` | 400 / 404 |
| GRN | `goods.receipt.note.*` (+ `invoice.required`, `location.required`) | 400 / 404 |
| GIN | `goods.issue.note.*` | 400 / 404 |
| Batch | `stock.batch.not.found`, `stock.batch.insufficient` | 404 / 400 |
| Shrinkage | `stock.shrinkage.*` | 400 / 404 |

PR / StockTake có thể dùng thêm `CommonErrorCode` (NOT_FOUND, CONFLICT, INVALID_REQUEST).

---

## 16. Cross-links

| Module | Liên kết | Cách dùng |
|--------|----------|-----------|
| `module-product-bom` | `ProductRepository`, `warning_threshold`, `expiryAlertDays`, category `LSP_VEG` | Tên SP, shelf life, low-stock |
| NCC / Customer / Categories | JDBC `nccs`, `customers`/`khs`, `categories` | GRN supplier, GIN customer, expiry |
| `module-approval-bom` | `ApprovalCreator`, `ApprovalDecidedEvent`, `SubjectType.PURCHASE_REQUEST` | Duyệt PR |
| `module-common` / server | `NotificationService`, `StockAlertNotifier` | Cảnh báo tồn / cận hạn |
| MinIO | `MinioService` | GRN/GIN attachments |
| System job | `SchedulableJob`, bảng `system_job` | `STOCK_ALERT_SCAN` |
| `module-accounting-bom` | **Chưa bridge** | `GrnConfirmedEvent` không có `@EventListener`; enum `PostingSource.PURCHASE` / `INVENTORY` là placeholder |

Guild liên quan:

- [module-accounting-bom.md](./module-accounting-bom.md) — journal / COA (roadmap GRN→GL)
- [DATABASE_STANDARD.md](../DATABASE_STANDARD.md) — naming / money NUMERIC / soft delete

---

## 17. Checklist

### 17.1 Đọc code lần đầu

- [ ] Hierarchy `Warehouse` → `Zone` → `Location`
- [ ] `stock_balances` key `(product, warehouse, location, batch)` + `stock_ledger` / `stock_transactions`
- [ ] GRN: submit → approve → **confirm** (batch + balance + ledger + `GrnConfirmedEvent`)
- [ ] GIN: FEFO suggest → confirm trừ lô
- [ ] Transfer / Adjustment / Shrinkage confirm paths
- [ ] StockTake `post-variance` = stub
- [ ] ReorderRule + `StockAlertJob` + `ExpiryAlertServiceImpl`
- [ ] PR from alerts → approval → PO from PR; `confirm-receive` = stub
- [ ] Attachments MinIO GRN/GIN
- [ ] `WarehouseErrorCode` + migration batch/shrinkage + system_job

### 17.2 Verify môi trường / nghiệp vụ

- [ ] Có ít nhất 1 kho ACTIVE + zone + location
- [ ] Product có `warning_threshold` / `expiryAlertDays` (nếu test alert)
- [ ] Job `STOCK_ALERT_SCAN` có trong `system_job` (hoặc gọi `/stock-alerts/scan`)
- [ ] `warehouse.pr.approval.required` khớp kỳ vọng duyệt
- [ ] Nhập chuẩn: GRN confirm (không chỉ PO confirm-receive)
- [ ] Xuất: FEFO suggest trước confirm GIN
- [ ] Role có permission `/warehouse/*` tương ứng

### 17.3 Rule bắt buộc

| ✅ | ❌ |
|----|----|
| Confirm GRN/GIN mới đổi tồn | Sửa `stock_balances` tay ngoài luồng |
| GRN có NCC/PO → có `invoice_no` trước confirm | Confirm thiếu hóa đơn |
| Dòng GRN qty > 0 → có `location_id` | Confirm thiếu vị trí |
| Nhập có lô qua GRN | Coi `PO.confirm-receive` là nhập chuẩn |
| Alert idempotency key / ngày | Tạo trùng alert cùng ngày không kiểm soát |
| Soft-delete PR/PO/rule | Hard-delete phiếu đã CONFIRMED |

---

## 18. Điểm WIP / roadmap

| Hạng mục | Trạng thái |
|----------|------------|
| StockTake `post-variance` → StockAdjustment | Stub |
| PO `confirm-receive` tăng tồn chuẩn (lô/vị trí) | Stub |
| ReorderRule API expose `preferred_supplier_id` | Thiếu trên create/update |
| Transfer cập nhật `stock_batch.qty_on_hand` | Chưa |
| GRN → Journal GL (`PostingSource.PURCHASE`) | Event có, listener chưa |
| Import Excel reorder-rules | Stub MVP |
| Optimistic lock tồn | Chưa (`@Version` / SELECT FOR UPDATE) |

---

*Cập nhật khi đổi workflow GRN/GIN, schema batch/alert, job scan, hoặc bridge kế toán.*
