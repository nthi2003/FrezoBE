# Phiếu nhập & xuất kho

Làm việc **nhập hàng (GRN/PNK)** và **xuất hàng (GIN/PXK)** theo quy trình kho–kế toán Việt Nam (T3, AMIS, Fast).

**Màn hình:**
- **Phiếu nhập kho** → `/warehouse/grn`
- **Phiếu xuất kho** → `/warehouse/gin`

---

## Quy trình chung (5 trạng thái)

| Bước | Trạng thái | Nút UI | Tồn kho |
|------|------------|--------|---------|
| 1 | **DRAFT** (Nháp) | **Lưu nháp** / **Tạo PNK/PXK** | Chưa đổi |
| 2 | **PENDING_APPROVAL** (Chờ duyệt) | **Gửi duyệt** | Chưa đổi |
| 3 | **APPROVED** (Đã duyệt) | **Duyệt** | Chưa đổi |
| 4 | **CONFIRMED** (Đã xác nhận) | **Xác nhận nhập** / **Xác nhận xuất** | **Đã cộng/trừ** |
| — | **CANCELLED** (Huỷ) | **Huỷ** | Không đổi |

Pipeline trên màn chi tiết: **Nháp → Chờ duyệt → Đã duyệt → Đã nhập/xuất kho**.

---

## Phiếu nhập kho (GRN / PNK)

Block **Hóa đơn NCC & PO** trên chi tiết: NCC, Số HĐ, Ngày HĐ, liên kết PO.

1. **Tạo PNK** — số HĐ NCC, dòng hàng.
2. **Gửi duyệt** → **Duyệt**.
3. **Xác nhận nhập** → tồn tăng.

Demo: GRN-DEMO-001 (nháp), GRN-DEMO-002 (chờ duyệt), GRN-DEMO-003 (đã nhập).

---

## Phiếu xuất kho (GIN / PXK)

Block **Chứng từ xuất**: loại xuất, số CT/HĐ, khách, kho đích.

1. **Tạo PXK** → **Gửi duyệt** → **Duyệt** → **Xác nhận xuất**.

Demo: GIN-DEMO-001 (nháp), GIN-DEMO-002 (chờ duyệt), GIN-DEMO-003 (đã xuất).
