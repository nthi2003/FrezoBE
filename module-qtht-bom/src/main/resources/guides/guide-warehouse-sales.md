# Đơn hàng, hoá đơn bán & tồn kho

Biết **chỗ nào đổi số lượng kho**, chỗ nào chỉ là giấy tờ bán hàng — tránh tưởng xuất hoá đơn là đã trừ tồn.

**Ảnh minh hoạ:** `/docs-assets/eu/` (FTECH demo)

![Menu — nhóm kho / CRM](/docs-assets/eu/menu-sidebar.png)

---

## Câu trả lời ngắn (đọc trước)

| Việc bạn làm | Tồn kho đổi? | Ghi chú |
|--------------|--------------|---------|
| Tạo / duyệt **Yêu cầu mua (PR)** | **Không** | Chỉ đổi trạng thái PR |
| Tạo **Đơn mua (PO)** | **Không** | Chưa nhập hàng |
| **Xác nhận phiếu nhập (GRN)** | **Có — tăng** | Số lượng + ngày giờ trên sổ kho |
| **Xác nhận phiếu xuất (GIN)** | **Có — giảm** | Trừ tồn khi xác nhận GIN |
| Tạo đơn / **Xuất hoá đơn** CRM | **Không** | Chỉ đổi trạng thái hoá đơn (Nháp → Đã xuất) |
| Ghi sổ GL từ hoá đơn | **Không** (kho) | Ghi kế toán doanh thu / phải thu — **không** trừ tồn |

> **Lưu ý EU:** Hiện Frezo **chưa tự trừ kho** khi bấm xuất hoá đơn. Muốn giảm tồn phải làm **Phiếu xuất kho (GIN)** và **Xác nhận**.

![Danh sách hoá đơn CRM (tham chiếu)](/docs-assets/eu/acc-crm-invoices.png)

---

## Làm việc chính — bán hàng (CRM)

1. **CRM & Khách hàng** → **Hoá đơn** (`/crm/invoices`).
2. Tạo hoá đơn nháp: chọn khách, dòng hàng (tên / SL / đơn giá) → lưu.
3. Bấm **Xuất hoá đơn** (hoặc tương đương trên màn) → trạng thái **Đã xuất**.
4. (Tuỳ quy trình kế toán) **Ghi sổ** → tạo bút toán phải thu / doanh thu.

**Kết quả:** Hoá đơn đổi trạng thái; có thể ghi sổ kế toán. **Số lượng kho không đổi** ở bước này.

---

## Làm việc chính — kho tăng / giảm thật

### Nhập hàng (tăng tồn)

1. Có **Yêu cầu mua** đã duyệt (nếu công ty dùng duyệt PR) → tạo **Đơn mua** khi cần.
2. **Kho** → **Phiếu nhập (GRN)** → nhập dòng hàng / kho.
3. Bấm **Xác nhận** phiếu nhập.

**Kết quả:** Tồn trên tay **tăng**; có giao dịch sổ kho kèm thời điểm xác nhận.

### Xuất hàng (giảm tồn)

1. **Kho** → **Phiếu xuất (GIN)** → chọn kho, dòng hàng, số lượng.
2. Bấm **Xác nhận** phiếu xuất.

**Kết quả:** Tồn trên tay **giảm** đúng số đã xác nhận. Nếu thiếu tồn, hệ thống báo không đủ — không trừ âm.

---

## Luồng mua hàng thường gặp (FTECH)

Ví dụ PR *HTX Rau Sạch Đà Lạt* · *Rau Cải Xanh* · SL *200*:

1. Gửi PR → duyệt ở **Hộp thư duyệt** → PR **Đã duyệt** (chưa vào kho).
2. Tạo PO / nhận hàng thực tế ngoài app hoặc trên app.
3. Tạo **GRN** → **Xác nhận** → tồn *Rau Cải Xanh* tăng *200* (theo dòng xác nhận).

Duyệt PR **không** thay bước 3.

---

## Lỗi / hiểu nhầm thường gặp

| Mong đợi | Thực tế hiện tại |
|----------|------------------|
| Xuất hoá đơn → tồn giảm ngay | **Chưa** — phải làm GIN và xác nhận |
| Duyệt PR → hàng đã trong kho | **Chưa** — cần GRN xác nhận |
| Xuất hoá đơn = phiếu xuất kho | **Hai việc khác nhau** (CRM vs Kho) |

Khi công ty cần “một nút xuất HĐ + trừ kho”, ghi nhận là **gap sản phẩm** — hỏi Admin / BA trước khi vận hành như đã có.

→ [Cấu hình luồng duyệt](/docs/guide-approval-flows) · [Gắn luồng mua](/docs/guide-approval-attach) · [Cơ hội bán](/docs/guide-deals)
