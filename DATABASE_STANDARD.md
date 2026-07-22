  # Frezo Backend — Database Standard

> Chuẩn PostgreSQL / naming / migration / index / audit / soft delete / query optimization cho FrezoBE.
> Đọc **cùng** [AI_BACKEND_ENGINEERING_GUIDE.md](./AI_BACKEND_ENGINEERING_GUIDE.md) và [SPRING_BOOT_BEST_PRACTICE.md](./SPRING_BOOT_BEST_PRACTICE.md).

DBMS: **PostgreSQL 14+**. Không hỗ trợ MySQL/Oracle/SQL Server làm primary DB.

---

## 1. Naming Convention

### 1.1 Bảng

| Rule | Ví dụ |
|------|-------|
| **snake_case**, tiếng Anh | `departments`, `purchase_orders`, `stock_movements` |
| **Số nhiều** (chỉ tập hợp) | `users` ✅ / `user` ❌ |
| Bảng join many-to-many: `<a>_<b>` (alphabet) | `role_permissions`, `user_roles` |
| Bảng lookup / master data: prefix `dm_` (danh mục) hoặc `ref_` | `dm_cities`, `ref_currencies` |
| Bảng log/audit: suffix `_log` | `audit_log`, `api_call_log` |
| Bảng history/snapshot: suffix `_history` | `contract_history`, `product_price_history` |
| Bảng tạm/queue: suffix `_queue` | `email_queue`, `notification_queue` |

**Cấm:**
- ❌ Tên bảng viết tắt kiểu `usr`, `dept`, `pdt`
- ❌ Tên bảng có prefix `tbl_` hoặc `t_`
- ❌ CamelCase (`Users`, `PurchaseOrders`) — PostgreSQL mặc định lowercase, tránh quote identifier

### 1.2 Cột

| Rule | Ví dụ |
|------|-------|
| **snake_case** | `first_name`, `phone_number`, `created_at` |
| **Bằng danh từ**, không verb | `email` ✅ / `get_email` ❌ |
| Boolean prefix `is_/has_/can_` | `is_deleted`, `is_active`, `has_children` |
| Date-only: suffix `_date` | `birth_date`, `signed_date` |
| Timestamp: suffix `_at` | `created_at`, `updated_at`, `deleted_at` |
| Foreign key: `<entity>_id` (số ít) | `organization_id`, `parent_id`, `manager_id` |
| ID chính: `id` (KHÔNG `department_id` trong bảng `departments`) | `id UUID PRIMARY KEY` |
| Số lượng: suffix `_count` | `member_count`, `retry_count` |
| Tiền: suffix `_amount` hoặc `_price`, kèm cột `_currency` | `total_amount`, `unit_price`, `currency` |
| JSON: suffix `_data` hoặc `_metadata` (kiểu JSONB) | `payload_data`, `settings_metadata` |

### 1.3 Constraint & Index

| Loại | Format | Ví dụ |
|------|--------|-------|
| Primary key | `pk_<table>` (thường auto) | `pk_departments` |
| Foreign key | `fk_<table>_<column>` | `fk_departments_organization_id` |
| Unique | `uq_<table>_<col1>_<col2>` | `uq_departments_org_code` |
| Index | `idx_<table>_<col1>_<col2>` | `idx_departments_status_created_at` |
| Check | `ck_<table>_<column>` | `ck_orders_total_amount_positive` |
| Partial index | `idx_<table>_<col>_<condition>` | `idx_departments_active_only` (WHERE is_deleted = false) |
| GIN index (JSONB, tsvector) | `gin_<table>_<column>` | `gin_articles_search_vector` |

### 1.4 Sequence / Trigger / Function

| Loại | Format |
|------|--------|
| Sequence | `seq_<table>_<column>` (thường không dùng do UUID) |
| Trigger | `trg_<table>_<when>_<action>` — `trg_orders_before_insert` |
| Function | `fn_<action>_<noun>` — `fn_calculate_order_total` |
| View | `v_<name>` — `v_active_departments` |
| Materialized view | `mv_<name>` — `mv_sales_daily` |

---

## 2. Primary Key — UUID String

**Chuẩn hiện tại (giữ nguyên):**
- Kiểu: `VARCHAR(36)` (UUID canonical form)
- Sinh: `@PrePersist` trong `BaseEntity` gọi `UUID.randomUUID().toString()`
- KHÔNG dùng `SERIAL`, `BIGSERIAL`, `IDENTITY` — leak thứ tự tạo, khó merge, khó shard

**Vì sao UUID String (36 char) thay vì UUID native (16 byte):**
- Compatibility với JSON, log, URL (không cần convert)
- Frontend TypeScript xử lý string dễ hơn Uint8Array
- Trade-off: 36 byte thay vì 16 byte + index chậm hơn ~15% — chấp nhận được cho scale hiện tại

**Alternative (cân nhắc tương lai):** UUIDv7 (time-ordered, index-friendly hơn v4).

---

## 3. Audit Fields — bắt buộc

Mọi table business kế thừa từ `BaseEntity` phải có:

```sql
id           VARCHAR(36)  PRIMARY KEY,
created_by   VARCHAR(50)  NOT NULL,
created_date TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
updated_by   VARCHAR(50),
updated_date TIMESTAMPTZ,
is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE,
version      BIGINT       NOT NULL DEFAULT 0            -- optimistic locking
```

**Bổ sung khuyến nghị:**
```sql
deleted_at   TIMESTAMPTZ,                                -- soft delete timestamp
deleted_by   VARCHAR(50)                                 -- ai xóa
```

**Bảng lookup / reference / master data ít thay đổi** có thể bỏ `updated_by/updated_date/version` để nhẹ (tuỳ trường hợp — mặc định cứ thêm cho nhất quán).

---

## 4. Foreign Key

### 4.1 Rule

- **Bắt buộc** khai báo FK trong DB (không chỉ trong entity JPA)
- **ON DELETE** rule tùy semantic:
  - `RESTRICT` (default) — không cho xóa parent nếu còn child
  - `CASCADE` — chỉ khi child KHÔNG có ý nghĩa độc lập (Order → OrderLine)
  - `SET NULL` — khi child cho phép orphan (parent_id có thể null)
- **ON UPDATE**: hầu hết là `CASCADE` (id không nên đổi vì UUID)

### 4.2 Ví dụ

```sql
CREATE TABLE departments (
    id                VARCHAR(36) PRIMARY KEY,
    code              VARCHAR(32) NOT NULL,
    name              VARCHAR(255) NOT NULL,
    status            VARCHAR(32) NOT NULL,
    organization_id   VARCHAR(36) NOT NULL,
    parent_id         VARCHAR(36),
    manager_id        VARCHAR(36),
    created_by        VARCHAR(50) NOT NULL,
    created_date      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(50),
    updated_date      TIMESTAMPTZ,
    is_deleted        BOOLEAN     NOT NULL DEFAULT FALSE,
    version           BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT fk_departments_organization_id
        FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_departments_parent_id
        FOREIGN KEY (parent_id)       REFERENCES departments(id)   ON DELETE SET NULL,
    CONSTRAINT fk_departments_manager_id
        FOREIGN KEY (manager_id)      REFERENCES persons(id)       ON DELETE SET NULL,

    CONSTRAINT uq_departments_org_code UNIQUE (organization_id, code)
);
```

### 4.3 Cấm

- ❌ Bỏ khai báo FK trong DB (chỉ dựa vào `@ManyToOne` — orphan record khi bug)
- ❌ `ON DELETE CASCADE` giữa 2 bảng thuộc 2 aggregate khác nhau (dễ mất data cascade)
- ❌ Circular FK (a → b → a) — refactor bằng bảng join

---

## 5. Index Strategy

### 5.1 Bắt buộc có index cho

1. **Mọi foreign key** — JOIN sẽ chậm nếu không index (PostgreSQL không auto-create như MySQL)
2. **Cột WHERE** thường xuyên (status, is_deleted, tenant_id, user_id)
3. **Cột ORDER BY** thường xuyên (created_at, updated_at)
4. **Cột UNIQUE** (đã tự có index)
5. **Cột GROUP BY** trên bảng lớn

### 5.2 Composite index — thứ tự cột QUAN TRỌNG

**Rule:** cột equality (`=`, `IN`) đứng trước cột range (`<`, `>`, `BETWEEN`, `LIKE`), cột `ORDER BY` đứng cuối.

Ví dụ query:
```sql
SELECT * FROM orders
WHERE tenant_id = ? AND status = ? AND created_at > ?
ORDER BY created_at DESC
LIMIT 20;
```

Index tốt:
```sql
CREATE INDEX idx_orders_tenant_status_created
    ON orders(tenant_id, status, created_at DESC);
```

Order matter: `(tenant_id, status, created_at)` — không phải `(created_at, tenant_id, status)`.

### 5.3 Partial index — soft delete

Vì filter `is_deleted = false` gần như MỌI query, tạo partial index tiết kiệm 20-40% dung lượng:

```sql
CREATE INDEX idx_departments_active_code
    ON departments(code)
    WHERE is_deleted = false;
```

### 5.4 GIN index — full-text search & JSONB

```sql
-- Full-text search tiếng Việt
ALTER TABLE articles ADD COLUMN search_vector tsvector;
UPDATE articles SET search_vector = to_tsvector('simple', unaccent(coalesce(title,'') || ' ' || coalesce(content,'')));
CREATE INDEX gin_articles_search_vector ON articles USING GIN(search_vector);

-- JSONB query
CREATE INDEX gin_orders_metadata ON orders USING GIN(metadata jsonb_path_ops);
```

### 5.5 Cấm

- ❌ Index cho cột low-cardinality (`is_deleted` đơn lẻ, `status` chỉ 3 giá trị) — trừ khi partial index
- ❌ Tạo quá nhiều index (mỗi INSERT/UPDATE phải maintain) — quy tắc ngón tay cái: max 5 index per bảng
- ❌ Index cột `TEXT` dài mà không dùng partial/expression index (dung lượng lớn)
- ❌ Duplicate index (`(a, b)` và `(a)` — index đầu đã cover query trên `a`)

### 5.6 Monitor

```sql
-- Index không dùng
SELECT schemaname, relname AS table_name, indexrelname AS index_name, idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0 AND indexrelname NOT LIKE 'pk_%' AND indexrelname NOT LIKE 'uq_%';

-- Query chậm (bật pg_stat_statements)
SELECT query, calls, mean_exec_time, total_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC LIMIT 20;
```

---

## 6. Migration — Flyway

### 6.1 Bắt buộc chuyển từ `ddl-auto: update` sang Flyway

**Vấn đề hiện tại:**
- `spring.jpa.hibernate.ddl-auto: update` — không có history, không rollback, dev/staging/prod schema drift
- `spring.flyway.enabled: false` — chưa dùng
- Seed data file rời rạc không version (`menu_data.sql`, `role_data.sql` — không có prefix `V1__`)

**Fix:**

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 0
    locations: classpath:db/migration
    validate-on-migrate: true
    out-of-order: false                       # ❌ không cho phép migrate out-of-order production
  jpa:
    hibernate:
      ddl-auto: validate                      # ✅ chỉ validate, không auto-DDL
```

### 6.2 File naming

```
module-server/src/main/resources/db/migration/
├── V202601010001__baseline_schema.sql
├── V202601020001__add_departments_table.sql
├── V202601050001__add_index_departments_org_code.sql
├── V202601100001__seed_menu.sql
├── V202602010001__add_column_departments_version.sql
├── R__view_active_departments.sql           # Repeatable — re-run khi content thay đổi
└── U202602010001__add_column_departments_version.sql  # Undo (không dùng OSS Flyway free)
```

**Rule naming:**
- Prefix `V<yyyyMMddHHmm>__` (timestamp UTC) — dễ order, tránh conflict giữa branch
- `__` (2 dấu gạch dưới) ngăn cách timestamp và description
- Description snake_case, mô tả rõ (`add_departments_table` không phải `changes`)
- Không dùng `V1__`, `V2__` (dễ conflict merge giữa 2 branch)

### 6.3 Rule migration

| Rule | Chi tiết |
|------|----------|
| **Không sửa migration đã apply** | Đã run staging/prod → tạo migration mới, không sửa file cũ (checksum mismatch → Flyway fail) |
| **Idempotent khi có thể** | `CREATE TABLE IF NOT EXISTS`, `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` |
| **Không destructive default** | `DROP COLUMN`, `DROP TABLE` cần confirm PR + backup |
| **Migration ngắn** | 1 file = 1 mục đích. Không nhét 5 thay đổi vào 1 file (khó rollback) |
| **DDL + DML tách file** | `V...__add_column.sql` (DDL) và `V...__seed_data.sql` (DML) riêng |
| **Long-running migration** | Backfill data lớn → tách job riêng, không block deploy |
| **Rollback plan** | Mỗi migration destructive phải có kế hoạch rollback (undo script hoặc rollback deploy trước) |

### 6.4 Ví dụ

```sql
-- V202607160001__add_departments_table.sql
CREATE TABLE IF NOT EXISTS departments (
    id                VARCHAR(36) PRIMARY KEY,
    code              VARCHAR(32) NOT NULL,
    name              VARCHAR(255) NOT NULL,
    status            VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    organization_id   VARCHAR(36) NOT NULL,
    parent_id         VARCHAR(36),
    manager_id        VARCHAR(36),
    created_by        VARCHAR(50) NOT NULL,
    created_date      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(50),
    updated_date      TIMESTAMPTZ,
    is_deleted        BOOLEAN     NOT NULL DEFAULT FALSE,
    version           BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT fk_departments_organization_id
        FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_departments_parent_id
        FOREIGN KEY (parent_id)       REFERENCES departments(id)   ON DELETE SET NULL,
    CONSTRAINT fk_departments_manager_id
        FOREIGN KEY (manager_id)      REFERENCES persons(id)       ON DELETE SET NULL,
    CONSTRAINT uq_departments_org_code UNIQUE (organization_id, code)
);

CREATE INDEX IF NOT EXISTS idx_departments_status
    ON departments(status) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_departments_parent_id
    ON departments(parent_id) WHERE parent_id IS NOT NULL;

COMMENT ON TABLE  departments IS 'Phòng ban thuộc tổ chức';
COMMENT ON COLUMN departments.code   IS 'Mã phòng ban, unique trong 1 organization';
COMMENT ON COLUMN departments.status IS 'ACTIVE | INACTIVE | ARCHIVED';
```

**Kèm COMMENT** cho bảng và cột quan trọng — self-documenting, hữu ích cho SELECT/pgAdmin/DBeaver.

---

## 7. Data Types

### 7.1 Bảng chọn kiểu dữ liệu

| Data | PostgreSQL | Java (JPA) | Ghi chú |
|------|-----------|-----------|---------|
| ID | `VARCHAR(36)` | `String` | UUID canonical |
| Short string (code, status) | `VARCHAR(N)` với N cụ thể | `String` | Không `TEXT` cho field bắt buộc constraint size |
| Long text (description, content) | `TEXT` | `String` | Không limit size trong app |
| Boolean | `BOOLEAN` | `Boolean` | Không `INT 0/1`, không `CHAR('Y'/'N')` |
| Timestamp | `TIMESTAMPTZ` | `OffsetDateTime` hoặc `Instant` | **BẮT BUỘC** timezone aware. KHÔNG `TIMESTAMP` |
| Date only | `DATE` | `LocalDate` | |
| Time only | `TIME` | `LocalTime` | Ít dùng |
| Money | `NUMERIC(19, 4)` | `BigDecimal` | **CẤM** `FLOAT`, `DOUBLE`, `REAL` cho tiền |
| Quantity | `NUMERIC(18, 3)` | `BigDecimal` | |
| Percent | `NUMERIC(5, 2)` | `BigDecimal` | 0-100.00 |
| Integer | `INTEGER` / `BIGINT` | `Integer` / `Long` | Đủ range |
| Enum | `VARCHAR(32)` + `@Enumerated(EnumType.STRING)` | Java enum | KHÔNG `EnumType.ORDINAL` (thứ tự đổi → data sai) |
| JSON | `JSONB` (KHÔNG `JSON`) | `String` / Hibernate `@Type(JsonBinaryType.class)` | JSONB có index GIN được |
| Binary | `BYTEA` | `byte[]` | File lớn → MinIO, không lưu DB |
| Array | `VARCHAR(N)[]` | `List<String>` (Hibernate `@Type(ListArrayType.class)`) | Hoặc dùng bảng join — thường tốt hơn |
| Geolocation | `POINT` hoặc PostGIS `GEOGRAPHY(POINT, 4326)` | Custom | Cần PostGIS extension |
| IP address | `INET` | `String` | Support IPv4 + IPv6 |
| UUID native | `UUID` | `UUID` | Nếu muốn đổi từ `VARCHAR(36)` |

### 7.2 Timezone

**Bắt buộc:** DB lưu **UTC**. FE quy đổi sang timezone local hiển thị.

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
  datasource:
    hikari:
      data-source-properties:
        stringtype: unspecified               # tương thích JSONB
```

App JVM cũng nên set `-Duser.timezone=UTC` để đồng nhất.

---

## 8. Soft Delete

### 8.1 Rule

- Mọi entity business → có `is_deleted BOOLEAN DEFAULT FALSE`
- Thêm `deleted_at TIMESTAMPTZ`, `deleted_by VARCHAR(50)` để trace
- Query mặc định `WHERE is_deleted = false`
- Hard delete chỉ có ở data retention job (theo compliance)

### 8.2 Cách implement — 3 chọn lựa

**Cách 1: Hibernate `@Where` (đơn giản, có drawback)**
```java
@Entity
@Where(clause = "is_deleted = false")
public class Department extends BaseEntity { ... }
```
Drawback: filter apply MỌI query, kể cả JOIN — muốn query bao gồm deleted phải native SQL.

**Cách 2: Hibernate `@SQLDelete` + `@Where` (recommended)**
```java
@Entity
@SQLDelete(sql = "UPDATE departments SET is_deleted = true, deleted_at = NOW(), deleted_by = ? WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Department extends BaseEntity { ... }
```
`repository.delete(entity)` tự động soft delete.

**Cách 3: Method riêng trong Repository (rõ ràng hơn, ưu tiên)**
```java
public interface DepartmentRepository extends JpaRepository<Department, String>, ... {

    @Query("select d from Department d where d.isDeleted = false")
    Page<Department> findAllActive(Specification<Department> spec, Pageable pageable);

    @Modifying
    @Query("update Department d set d.isDeleted = true, d.deletedAt = :now, d.deletedBy = :who where d.id = :id")
    int softDelete(@Param("id") String id, @Param("now") OffsetDateTime now, @Param("who") String who);
}
```

Chọn **Cách 3** — explicit, dễ debug, không magic.

### 8.3 Unique constraint với soft delete

Vấn đề: `UNIQUE (code)` sẽ chặn tạo mới nếu record cũ đã soft-deleted với cùng code.

**Fix:** Partial unique index bỏ qua deleted:
```sql
CREATE UNIQUE INDEX uq_departments_org_code_active
    ON departments(organization_id, code)
    WHERE is_deleted = false;
```

---

## 9. Query Optimization

### 9.1 EXPLAIN ANALYZE — luôn chạy trước khi ship query mới

```sql
EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
SELECT d.id, d.name, o.name AS org_name
FROM departments d
LEFT JOIN organizations o ON o.id = d.organization_id
WHERE d.is_deleted = false AND d.status = 'ACTIVE'
ORDER BY d.created_date DESC
LIMIT 20;
```

Check:
- `Seq Scan` trên bảng lớn → thiếu index
- `Nested Loop` với thousand rows → thiếu index
- `Sort` (Memory: xxx) → thêm index cho `ORDER BY`
- `Buffers: shared read=xxxx` → cold cache, ok; `hit=xxxx` → cached, tốt

### 9.2 Rules

| Rule | Chi tiết |
|------|----------|
| **Không `SELECT *`** production | Chỉ lấy cột cần — giảm I/O, giảm network |
| **Pagination bắt buộc** | Không `SELECT ... FROM huge_table` không LIMIT |
| **`EXISTS` > `IN` cho subquery lớn** | `WHERE EXISTS (SELECT 1 FROM ...)` |
| **`COUNT(*)` chậm trên bảng lớn** | Cache count, hoặc dùng `pg_class.reltuples` cho estimate |
| **`LIKE '%xxx%'`** dùng GIN index (`pg_trgm` extension) hoặc full-text search | KHÔNG scan toàn bảng |
| **Batch UPDATE/DELETE** | Chia chunk 1000 rows, tránh lock table lâu |
| **`ORDER BY random()`** cực chậm trên bảng lớn | Dùng offset random hoặc `TABLESAMPLE` |
| **`OFFSET N`** chậm khi N lớn (> 10000) | Chuyển sang keyset pagination (`WHERE id > last_id ORDER BY id LIMIT N`) |
| **JOIN tránh nhiều bảng lớn** | > 5 bảng lớn → cân nhắc denormalize hoặc materialized view |
| **JSONB query dùng index GIN** | `WHERE metadata @> '{"key": "value"}'` với index `gin(metadata jsonb_path_ops)` |

### 9.3 Cấm

- ❌ `SELECT * FROM huge_table` không WHERE
- ❌ `WHERE UPPER(email) = ?` (không dùng index) → tạo functional index hoặc lưu lowercase
- ❌ `WHERE cast_type(col) = ?` → cast làm mất index
- ❌ Cross join không có ON clause
- ❌ `DISTINCT` để "khử trùng" JOIN N+1 (fix ở data model)

### 9.4 pg_stat_statements — bật production

```yaml
# postgresql.conf
shared_preload_libraries = 'pg_stat_statements'
pg_stat_statements.track = all
```

Weekly review:
```sql
SELECT query, calls, total_exec_time, mean_exec_time, rows
FROM pg_stat_statements
ORDER BY total_exec_time DESC LIMIT 20;
```

---

## 10. Enum — Reference Table vs `VARCHAR + @Enumerated`

| Kịch bản | Chọn |
|----------|------|
| Enum cố định, code control | `VARCHAR(32) + @Enumerated(EnumType.STRING)` |
| Enum thay đổi runtime, admin quản lý | Reference table `dm_xxx` |
| Enum có metadata (icon, color, description) | Reference table hoặc enum + config file |
| Enum dùng cross-service | Reference table (single source of truth) |

**Ví dụ enum code control:**
```java
public enum ContractStatus {
    DRAFT, PENDING_APPROVAL, APPROVED, ACTIVE, EXPIRED, TERMINATED, CANCELLED
}
```
DB: `status VARCHAR(32) NOT NULL`, check constraint:
```sql
ALTER TABLE contracts ADD CONSTRAINT ck_contracts_status
    CHECK (status IN ('DRAFT','PENDING_APPROVAL','APPROVED','ACTIVE','EXPIRED','TERMINATED','CANCELLED'));
```

**Cấm:** `EnumType.ORDINAL` — thứ tự enum trong Java thay đổi → data DB sai semantic.

---

## 11. Multi-tenancy (chuẩn bị sẵn)

Khi FrezoBE mở SaaS multi-tenant, chọn 1 trong 3 chiến lược:

| Chiến lược | Ưu | Nhược |
|-----------|-----|-------|
| **Shared DB, shared schema + tenant_id column** | Đơn giản, chi phí thấp | Rò rỉ data risk cao — bắt buộc row-level filter |
| Shared DB, schema per tenant | Isolation tốt hơn | Migration phức tạp (N schema) |
| Isolated DB per tenant | Isolation cao nhất | Chi phí cao, khó cross-tenant analytics |

**Nếu chọn shared + tenant_id (khuyến nghị bắt đầu):**
- Bảng business đều có `tenant_id VARCHAR(36) NOT NULL`
- Composite index luôn bắt đầu bằng `tenant_id`
- Interceptor Hibernate hoặc Filter tự động add `WHERE tenant_id = :current`
- Row-Level Security (PostgreSQL RLS) làm tầng bảo vệ thứ 2:
  ```sql
  ALTER TABLE departments ENABLE ROW LEVEL SECURITY;
  CREATE POLICY dept_tenant_policy ON departments
      USING (tenant_id = current_setting('app.current_tenant')::text);
  ```

---

## 12. Backup & Retention

| Loại | Chiến lược |
|------|-----------|
| Full backup | Daily 2 AM, giữ 30 ngày, S3 encrypted |
| Incremental (WAL archiving) | Every 15 min, giữ 7 ngày |
| Point-in-time recovery | Bật WAL archiving |
| Audit log retention | 7 năm (compliance), archive S3 Glacier |
| Soft-deleted records | 90 ngày → hard delete (job scheduled) |
| API log | 90 ngày → aggregate + drop chi tiết |

Test restore hàng quý (backup không restore được = không có backup).

---

## 13. Migration Checklist — từ code hiện tại

- [ ] Thêm Flyway dep vào `module-server/pom.xml`
- [ ] Tắt `spring.jpa.hibernate.ddl-auto: update`, đổi sang `validate`
- [ ] Bật `spring.flyway.enabled: true`
- [ ] Dump schema hiện tại → tạo `V202607010000__baseline_schema.sql`
- [ ] Chuyển các file `menu_data.sql`, `role_data.sql`, `role_menu_data.sql`, `organization_data.sql` thành `V202607010001__seed_menu.sql` v.v.
- [ ] Thêm `@Version` cột cho entity có concurrent edit (Contract, Order, Product, Stock, User)
- [ ] Thêm `deleted_at`, `deleted_by` vào `BaseEntity`
- [ ] Rà soát FK: bổ sung index cho mọi FK còn thiếu
- [ ] Rà soát column: thêm partial unique index cho `code` khi có soft delete
- [ ] Enable `pg_stat_statements` production
- [ ] Xóa `DriverManager.getConnection(...)` trong `FrezoServerApplication.main()` — dùng Flyway `V1__` thay
- [ ] Đổi timezone JVM + Hibernate về UTC
- [ ] Rà soát cột `TIMESTAMP` → chuyển `TIMESTAMPTZ` (nếu có)
- [ ] Rà soát cột tiền: KHÔNG `FLOAT/DOUBLE`, phải `NUMERIC(19,4)`

---

*Cập nhật khi thêm entity mới, đổi schema, tối ưu query, chuyển chiến lược multi-tenancy.*
