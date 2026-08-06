# Frezo Backend — module-product-bom (Sản phẩm / Giá / Lô / Giỏ / Đơn)

> Danh mục sản phẩm rau củ, đơn vị tính, nhóm giá, nhập lô kho, dashboard giá/lợi nhuận, giỏ hàng và checkout tạo `sale_orders`.
> Đọc **cùng** [DATABASE_STANDARD.md](../DATABASE_STANDARD.md). Cross-module: customer (`customerId`), NCC/supplier (`supplierId` trên batch), auth (dep Maven), MinIO ảnh.

Package gốc: `com.frezo.product`. Module Maven: `module-product-bom`.

Context HTTP thường: `/api` → `/api/product`, `/api/product/cart`, `/api/product/order`.

**Lưu ý danh mục:** không có entity Category trong module — `Product.categoryId` là string ref (thường DMDC / FE gửi `category` map → `categoryId`).

---

## 1. Vai trò & phạm vi

| Hạng mục | Chi tiết |
|----------|----------|
| Product | CRUD + filter paging, soft-delete, view_count khi getById |
| Pricing | `price_groups` + `price_configs` theo unit; bulk update; calculate-price |
| Inventory | Nhập lô nhanh (`product_batches` + `inventory_logs` type IMPORT); cost-history |
| Dashboard | Profit chart, price fluctuation, market comparison (`market_prices`) |
| Cart / Order | Giỏ theo customer → checkout → `sale_orders` PENDING |
| Có entity, API mỏng | `PaymentTransaction` (Sepay) — không controller trong module này |

```
ProductServiceImpl (façade)
  ├─ ProductCommandService     — filter/CRUD
  ├─ ProductPricingService     — bulk price / calculate
  ├─ ProductInventoryService   — importBatch / costHistory
  ├─ ProductDashboardService   — charts
  └─ ProductImageService       — MinIO upload

CartService → getOrCreate(ACTIVE) / add / clear(COMPLETED)
OrderService.createOrderFromCart → SaleOrder PENDING + clear cart
```

---

## 2. Class map

### 2.1 Controllers

| Controller | Base path | Vai trò |
|------------|-----------|---------|
| `ProductController` | `/product` | Product + giá + lô + dashboard + upload ảnh |
| `CartController` | `/product/cart` | Giỏ theo customerId |
| `OrderController` | `/product/order` | Checkout + list orders |

### 2.2 Services (façade + helpers)

| Class | Vai trò |
|-------|---------|
| `ProductService` / `ProductServiceImpl` | Façade ≤ 5 deps → delegate |
| `ProductCommandService` | filter, getById (+viewCount), create/update/delete |
| `ProductPricingService` | bulkUpdatePrices, calculatePrice |
| `ProductInventoryService` | importBatch, getCostHistory |
| `ProductDashboardService` | stats / profit / fluctuation / market |
| `ProductImageService` | upload MinIO |
| `CartService` | Cart lifecycle |
| `OrderService` | Checkout từ cart |

### 2.3 Entities → bảng

| Entity | Bảng | Ghi chú |
|--------|------|---------|
| `Product` | `products` | code unique; category_id; price; view_count |
| `ProductUnit` | `product_units` | unit_name (kg/bó/thùng…), conversion_rate |
| `PriceGroup` | `price_groups` | RETAIL, AGENT, RESTAURANT… |
| `PriceConfig` | `price_configs` | Giá theo unit × group + effective/expiry |
| `Batch` | `product_batches` | Lô nhập, cost_price, supplier_id |
| `InventoryLog` | `inventory_logs` | IMPORT / EXPORT / SHRINKAGE |
| `MarketPrice` | `market_prices` | Giá chợ đầu mối theo ngày |
| `Carts` | `carts` | status OPEN/ACTIVE/CHECKED_OUT/… (code dùng ACTIVE/COMPLETED) |
| `CartItem` | (entity `CartItem`) | snapshot unit_price / total_price |
| `SaleOrder` | `sale_orders` | payment CASH/VIETQR; PENDING/PAID… |
| `PaymentTransaction` | `payment_transactions` | gateway SEPAY — chưa API |

### 2.4 Repositories

`ProductRepository`, `ProductUnitRepository`, `PriceGroupRepository`, `PriceConfigRepository`, `BatchRepository`, `InventoryLogRepository`, `MarketPriceRepository`, `CartRepository`, `CartItemRepository`, `SaleOrderRepository`.

### 2.5 DTOs chính

| DTO | Dùng cho |
|-----|----------|
| `ProductCreateRequest` / `ProductUpdateRequest` | Field `category` → map `categoryId` |
| `ProductFilterRequest` | keyword, categoryId, paging… |
| `ProductResponse` | `category` alias = categoryId |
| `PriceUpdateRequest` | bulk: productCode + unitPrices[] |
| `BatchImportRequest` | supplierId + items[] |
| `ProductDashboardStats` | stats tổng |

---

## 3. API map

### 3.1 Product — `/product`

| Method | Path | Action | Ghi chú |
|--------|------|--------|---------|
| `POST` | `/product/filter` | VIEW | Body `ProductFilterRequest` (null → empty filter) |
| `GET` | `/product` | VIEW | = filter rỗng |
| `GET` | `/product/{id}` | VIEW | Tăng `viewCount` |
| `POST` | `/product` | CREATE | |
| `PUT` | `/product/{id}` | UPDATE | |
| `DELETE` | `/product/{id}` | DELETE | Soft-delete |
| `POST` | `/product/bulk-update-prices` | UPDATE | List `PriceUpdateRequest` |
| `POST` | `/product/import-batch` | CREATE | Nhập kho nhanh |
| `GET` | `/product/calculate-price` | VIEW | `productCode`, `unitName`, `priceGroupCode` |
| `GET` | `/product/{id}/cost-history` | VIEW | Points từ `product_batches` |
| `GET` | `/product/dashboard/profit-chart` | VIEW | `days` default 7 |
| `GET` | `/product/dashboard/price-fluctuation` | VIEW | |
| `GET` | `/product/dashboard/market-comparison` | VIEW | |
| `POST` | `/product/upload-image` | CREATE | Multipart → MinIO bucket freo-prod |

### 3.2 Cart — `/product/cart`

| Method | Path | Action |
|--------|------|--------|
| `GET` | `/product/cart/{customerId}` | VIEW | Items của cart ACTIVE (tạo mới nếu chưa) |
| `POST` | `/product/cart/{customerId}/add` | CREATE | Params `productId`, `quantity`, `price` |
| `DELETE` | `/product/cart/{customerId}/clear` | DELETE | Xóa items + status COMPLETED |

### 3.3 Order — `/product/order`

| Method | Path | Action |
|--------|------|--------|
| `POST` | `/product/order/checkout/{customerId}` | CREATE | Params `staffId`, `paymentMethod` default `CASH` |
| `GET` | `/product/order` | VIEW | Tất cả `SaleOrder` |

---

## 4. Entity fields chính

### 4.1 Product (`products`)

| Field | Ý nghĩa |
|-------|---------|
| `code`, `name` | Unique code |
| `origin`, `season` | Nguồn gốc / mùa vụ |
| `imageUrl`, `categoryId`, `description` | |
| `warningThreshold`, `expiryAlertDays` | Cảnh báo tồn / hết hạn |
| `isActive`, `price`, `rating`, `isNew` | |
| `viewCount` | +1 mỗi `getById` |

### 4.2 Unit & Price

| Entity | Fields quan trọng |
|--------|-------------------|
| `ProductUnit` | `productId`, `unitName`, `conversionRate`, `isDefault` |
| `PriceGroup` | `code`, `name`, `isActive` |
| `PriceConfig` | `productUnitId`, `priceGroupId`, `price`, `effectiveDate`, `expiryDate` |

### 4.3 Batch & Inventory

| Field Batch | Ý nghĩa |
|-------------|---------|
| `batchCode` | Unique (gen `BATCH-{ts}-{productCode}`) |
| `supplierId` | NCC / supplier |
| `growingArea`, `importDate`, `expiryDate` | |
| `initialQuantity` / `currentQuantity` / `costPrice` | |

| InventoryLog.type | Ý nghĩa |
|-------------------|---------|
| `IMPORT` | Nhập (import-batch) |
| `EXPORT` | Xuất |
| `SHRINKAGE` | Hao hụt / hỏng |

### 4.4 Cart & SaleOrder

| Carts.status (code) | Ý nghĩa |
|---------------------|---------|
| `ACTIVE` | Đang mở (getOrCreate) |
| `COMPLETED` | Sau clear / checkout |

| SaleOrder | Ý nghĩa |
|-----------|---------|
| `orderCode` | `ORD-{timestamp}` |
| `totalAmount` | Sum unitPrice × qty |
| `paymentStatus` | PENDING (sau checkout) |
| `paymentMethod` | CASH / VIETQR… |
| `sepayOrderId`, `qrContent`, `qrImageUrl`, `paidAt` | Cổng thanh toán (field sẵn) |

---

## 5. Luồng chính

### 5.1 CRUD + filter sản phẩm

```
POST /product/filter → ProductCommandService.filter (Specification + paging)
GET /product/{id} → find → viewCount++ → ProductResponse (category alias)
DELETE → soft-delete (isDeleted)
```

MapStruct: request `category` ↔ entity `categoryId`; response `category` = `categoryId`.

### 5.2 Giá theo nhóm

```
bulkUpdatePrices:
  product by code → mỗi unitName: resolveOrCreate unit
  → priceGroup by code → upsert active PriceConfig

calculatePrice:
  product + unit + group → findActivePrice → Double | null
```

### 5.3 Nhập kho nhanh

```
importBatch(supplierId, items[]):
  for each item:
    Product by code
    Batch (cost, qty, expiry, growingArea)
    InventoryLog type=IMPORT note="Nhập kho nhanh từ chuyến xe"
```

`getCostHistory(productId)`: batch theo `importDate` ASC → `{ date, unitCost, qty, batchCode, supplierId, source: BATCH }`.

### 5.4 Cart → Order

```
getOrCreateCart(customerId, status=ACTIVE)
addToCart(productId, quantity, price)  // snapshot; productUnitId có thể null nếu API không set
checkout:
  items empty → RuntimeException("Cart is empty")
  SaleOrder PENDING + total + paymentMethod
  clearCart → delete items + cart COMPLETED
```

| Rule | |
|------|--|
| ⚠️ `CartService.addToCart` **không** set `productUnitId` / `quantity` trên builder đầy đủ như entity require — kiểm tra runtime/DB constraint khi sửa | |
| ⚠️ Comment entity Carts: OPEN/CHECKED_OUT; code dùng ACTIVE/COMPLETED | |
| ❌ Checkout **không** tạo `PaymentTransaction` / trừ kho | |

---

## 6. Dependencies (Maven)

| Artifact | Lý do |
|----------|-------|
| `module-common` | BaseEntity, ApiResponse, CheckPermission, Spec, Minio |
| `module-auth-bom` | Khai báo POM (auth context / security stack) |
| `spring-boot-starter-data-jpa` / `validation` / `web` | |
| MapStruct | `ProductMapper` |

---

## 7. Cross-links

| Module / file | Liên hệ |
|---------------|---------|
| [module-customer-bom.md](./module-customer-bom.md) | `customerId` cart/order; voucher validate khi bán (nếu FE gọi) |
| [module-crm-bom.md](./module-crm-bom.md) | QuoteItem/InvoiceItem.productId + productName snapshot |
| `module-dmdc-bom` | Category master cho `categoryId` (ngoài module) |
| `module-customer-bom` NCC | `Batch.supplierId` |
| [DATABASE_STANDARD.md](../DATABASE_STANDARD.md) | Naming `products`, `product_batches`, soft-delete |

```
[FE ERP Product / POS]
  → /api/product (+ filter, prices, batch, dashboard)
  → /api/product/cart/{customerId}
  → /api/product/order/checkout/{customerId}
[MinIO freo-prod]
  ← upload-image
[CRM Invoice]
  ← product_id string trên line items
```

---

## 8. Checklist

### 8.1 Đọc code lần đầu

- [ ] `ProductController` toàn bộ endpoint dashboard / price / batch
- [ ] `ProductServiceImpl` façade → 5 helper services
- [ ] Entities: Product, ProductUnit, PriceGroup, PriceConfig, Batch, InventoryLog, MarketPrice
- [ ] `CartController` + `CartService` status ACTIVE/COMPLETED
- [ ] `OrderController` + `OrderService.createOrderFromCart`
- [ ] Không có Category entity trong module — chỉ `categoryId`
- [ ] `PaymentTransaction` entity không controller

### 8.2 Rule bắt buộc

| ✅ | ❌ |
|----|----|
| Soft-delete product | Hard-delete product master |
| Giá theo unit × priceGroup (PriceConfig) | Chỉ tin cột `products.price` cho mọi nhóm KH |
| import-batch ghi InventoryLog IMPORT | Nhập lô không log |
| Checkout clear cart | Để cart ACTIVE sau khi đã tạo order |
| Hiểu category = string id | Expect CRUD category trong module-product |

### 8.3 Kiểm thử thủ công gợi ý

- [ ] Create product + filter theo categoryId
- [ ] bulk-update-prices → calculate-price trả đúng
- [ ] import-batch → cost-history có điểm
- [ ] Add cart → checkout → order PENDING + cart rỗng/COMPLETED
- [ ] upload-image trả URL MinIO
- [ ] Dashboard endpoints trả list/map không 500

---

*Cập nhật khi wire Sepay webhook (`PaymentTransaction`), trừ kho khi bán, hoặc chuẩn hóa status cart (ACTIVE vs OPEN).*
