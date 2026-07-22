# Accounting — Seed dữ liệu mẫu

Dùng cho FE/QA chạy các màn: **accounts**, **journals**, **ledger**, **trial-balance**,
**financial-statements**, **bank-reconciliation**, **settings**.

## Cách kích hoạt

### 1. Tự động (khuyến nghị — local)

`AccountingDataInitializer` chạy khi app start với profile **`local`** (default trong
`application.yml`) hoặc **`seed`**:

```bash
# Default local đã đủ
mvn -pl module-server spring-boot:run

# Hoặc bật rõ profile seed (staging/dev không dùng local)
SPRING_PROFILES_ACTIVE=seed mvn -pl module-server spring-boot:run
```

Idempotent: restart lại **không** tạo trùng journal / bank statement (check `idempotencyKey` + `fileName`).

**Không** chạy trên profile `prod`.

### 2. Thủ công qua API (khi initializer không chạy)

| Bước | Method | Endpoint |
|------|--------|----------|
| Settings + seed COA | `PUT` | `/accounting/setting` body `{ "standard": "TT133", "seedCoa": true }` |
| Chỉ seed COA | `POST` | `/accounting/accounts/seed?standard=TT133` |
| Fiscal year + 12 kỳ OPEN | `POST` | `/accounting/periods/ensure?year=2026` |

Journal / bank statement mẫu **chỉ** tạo bởi `AccountingDataInitializer` (không có endpoint seed riêng).
Muốn có lại sau khi xoá DB: restart app với profile `local` hoặc `seed`.

## Data mẫu tạo ra

| Loại | Chi tiết |
|------|----------|
| **Settings** | Chuẩn `TT133`, currency `VND`, mapping TK lương mặc định |
| **COA** | Hệ thống TK rút gọn TT133 (`CoaTT133`) — skip code đã có |
| **Fiscal** | Năm hiện tại + 12 kỳ `OPEN` |
| **Journal POSTED** | `SEED-ACC-OPENING` — Nợ 1111 / Có 411 = 100.000.000 (05/01) |
| **Journal POSTED** | `SEED-ACC-BANK-DEPOSIT` — Nợ 1121 / Có 1111 = 50.000.000 (ngày 10 tháng hiện tại) |
| **Journal POSTED** | `SEED-ACC-REVENUE` — Nợ 1121 / Có 5113 = 10.000.000 (ngày 12) |
| **Journal DRAFT** | `SEED-ACC-EXPENSE-DRAFT` — Nợ 6422 / Có 1111 = 2.000.000 (ngày 15) |
| **Bank recon** | Statement `seed-bank-statement.csv` trên TK **1121**, 3 lines (2 có thể match journal, 1 phí unmatched) |

## Màn FE map data

- **settings** → settings TT133
- **accounts** → COA TT133
- **journals** → 3 POSTED + 1 DRAFT
- **ledger / trial-balance / financial-statements** → số liệu từ 3 chứng từ POSTED (filter khoảng ngày năm hiện tại)
- **bank-reconciliation** → statement OPEN trên 1121

## Class liên quan

- `com.frezo.accounting.config.AccountingDataInitializer`
- `com.frezo.accounting.seed.CoaTT133` / `CoaTT99`
- `AccountService.seedChartOfAccounts`
- `FiscalPeriodService.ensureYear`
