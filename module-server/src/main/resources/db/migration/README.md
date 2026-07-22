# Flyway Migration Guide — FrezoBE

Chuẩn migration schema DB PostgreSQL cho Frezo, xem chi tiết ở `FrezoBE/DATABASE_STANDARD.md §6`.

---

## Trạng thái hiện tại (v1.1 — Batch D)

- Flyway dependency đã thêm vào `module-server/pom.xml`.
- `spring.flyway.enabled = false` mặc định — CHỜ baseline schema xong mới bật.
- `spring.jpa.hibernate.ddl-auto = update` mặc định — CẤM cho production, chỉ dùng dev/local.
- Folder này rỗng — cần tạo baseline `V202607160000__baseline_schema.sql` từ DB hiện tại.

---

## Rollout — 5 bước bật Flyway an toàn cho DB đã có data

### Bước 1 — Dump baseline schema từ DB đang chạy

Với DB `frezo` đang chạy (không dừng service):

```bash
# Chỉ schema, không data
pg_dump \
  --host=localhost --port=5432 \
  --username=postgres \
  --dbname=frezo \
  --schema-only \
  --no-owner --no-privileges \
  --no-tablespaces \
  --file=V202607160000__baseline_schema.sql

# Move file vào src/main/resources/db/migration/
mv V202607160000__baseline_schema.sql FrezoBE/module-server/src/main/resources/db/migration/
```

**Kiểm tra**: mở file `.sql`, xóa các dòng `CREATE DATABASE`, `\connect`, `ALTER DATABASE ... OWNER`, comment `--`. Chỉ giữ `CREATE TABLE`, `CREATE INDEX`, `ALTER TABLE ADD CONSTRAINT`, `CREATE SEQUENCE`.

### Bước 2 — Đánh dấu Flyway baseline

Với DB đã có sẵn schema và data:

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 202607160000
    baseline-description: "Baseline from existing schema"
    locations: classpath:db/migration
```

Chạy 1 lần — Flyway sẽ tạo bảng `flyway_schema_history` và mark baseline. **KHÔNG** apply lại file baseline vì schema đã tồn tại.

### Bước 3 — Chuyển sang Hibernate validate mode

Sau baseline OK, chuyển `spring.jpa.hibernate.ddl-auto`:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate    # thay cho 'update' — an toàn cho production
```

`validate` mode: Hibernate check schema DB khớp với entity, fail-fast khi mismatch. KHÔNG auto tạo/sửa cột.

### Bước 4 — Từ giờ, MỌI thay đổi schema qua Flyway migration

Đặt tên chuẩn: `V<yyyyMMddHHmm>__<description>.sql`

Ví dụ:
```
V202607160930__add_deleted_at_to_base_entity.sql
V202607170815__add_index_on_orders_customer_id.sql
V202607181402__create_table_invoice.sql
```

**Rules:**
- Tên file: `V<version>__<description>.sql` (double underscore giữa version và mô tả).
- Version: `yyyyMMddHHmm` UTC — dễ sort chronological, tránh conflict git merge.
- Mô tả: snake_case, ngắn gọn, verb-first (`add_*`, `create_*`, `drop_*`, `rename_*`).
- **CẤM** sửa file migration đã apply lên staging/prod. Muốn fix → tạo file mới.
- Mỗi file: chỉ 1 chủ đề. Không gộp 5 việc vào 1 file.

### Bước 5 — Quy trình PR migration

1. Viết migration `.sql` mới trong branch feature.
2. Test local với DB tương tự prod.
3. PR: reviewer check
   - Tên file đúng convention.
   - SQL có `IF NOT EXISTS` / `IF EXISTS` để idempotent.
   - Không dùng `DROP` mà không có backup plan.
   - Nếu rename column: dùng `ALTER TABLE ... RENAME COLUMN`, không `ADD + DROP` (mất data).
4. Merge → CI apply lên staging DB tự động.
5. Staging OK 1-2 ngày → deploy prod, migration auto-apply khi service start.

---

## Migration cần thiết (backlog Batch D)

### V202607211000__contract_status_varchar.sql (URGENT — local unblock 2026-07-21)

`contract.status` (và `contract_history.status_contarct`) từng là `smallint` vì Hibernate default
`EnumType.ORDINAL`. Entity/API/demo_data dùng VARCHAR + `EnumType.STRING` → insert seed fail:

`ERROR: column "status" is of type smallint but expression is of type character varying`

- File migration: `V202607211000__contract_status_varchar.sql` (idempotent; apply khi bật Flyway).
- Runtime repair khi `flyway.enabled=false`: `ContractStatusSchemaFixer` (`@Order(70)`), chạy trước
  `ContractDataInitializer` (`@Order(80)`).

### V202607160930__add_soft_delete_columns.sql
```sql
-- Thêm deleted_at, deleted_by cho MỌI bảng extend BaseEntity (opt-in per table)
-- Vì có ~50+ table, tạo function bash để generate migration này:
--   for t in $(psql -tAc "SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename NOT LIKE 'flyway_%'"); do
--     echo "ALTER TABLE $t ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ, ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(50);"
--   done > V202607160930__add_soft_delete_columns.sql
```

### V202607161100__add_optimistic_lock_version.sql
```sql
-- Chỉ áp dụng cho entity CẦN concurrent-safe: orders, invoices, contracts, stock_balance, warehouse_transaction...
-- KHÔNG bắt buộc cho mọi entity.
ALTER TABLE orders            ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE goods_receipt_note ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE stock_balance      ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
-- Sau đó edit entity Java: thêm @Version private Long version;
```

### V202607161300__add_composite_indexes.sql
```sql
-- Fix N+1 và list query slow. Đọc log slow query > 500ms để pick index.
CREATE INDEX IF NOT EXISTS idx_person_org_department
    ON person(org_id, department_id) WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_orders_customer_status
    ON orders(customer_id, status, created_date DESC) WHERE is_deleted = false;

-- Full-text VN search
CREATE INDEX IF NOT EXISTS idx_person_name_gin
    ON person USING GIN (to_tsvector('simple', name));
```

---

## Anti-patterns cấm

- **CẤM** dùng `spring.jpa.hibernate.ddl-auto: create` / `create-drop` / `update` cho staging/prod. Chỉ `validate` hoặc `none`.
- **CẤM** sửa file `V*.sql` sau khi merge vào main. Muốn fix → tạo `V*_fix.sql` mới.
- **CẤM** `Vxxx__flyway_repair.sql` — dùng `flyway repair` CLI thay vì tạo file rác.
- **CẤM** `DELETE FROM flyway_schema_history` để "reset" — sẽ mất history, không rollback được.
- **CẤM** dùng `psql -f script.sql` để apply schema thủ công lên prod — luôn qua Flyway.

---

## Rollback strategy

Flyway không hỗ trợ auto-rollback (undo migration là premium feature). Chiến lược:

1. **Blue-green deployment**: giữ version cũ ready, nếu migration lỗi → rollback deployment code, DB giữ nguyên schema mới (thường schema mới backward-compat với code cũ nếu design đúng).
2. **Expand/contract pattern**:
   - Expand: thêm cột mới, cả code cũ + mới đều đọc/ghi được.
   - Contract: sau khi code mới stable → tạo migration drop cột cũ.
3. **Backup pre-migration**: `pg_dump` trước mỗi deployment prod (đã có trong CI/CD).
