# Gắn luồng duyệt vào nghỉ / mua / lương

**Cấu hình luồng duyệt** → tick **Đang kích hoạt** → badge **Áp dụng: …** → gửi đơn FTECH kiểm chứng.

*(Admin / vai trò cấu hình)*

**Ảnh minh hoạ:** `/docs-assets/eu/` (FTECH demo)

![Đầu trang Cấu hình luồng duyệt — **Tạo luồng mới**](/docs-assets/eu/cta-wf-flows-header.png)

![Menu — Cấu hình luồng duyệt](/docs-assets/eu/wf-sidebar-approval.png)

| Việc | Menu |
|------|------|
| **Áp runtime** | **Cấu hình luồng duyệt** → tab **Luồng đang chạy** |
| Vẽ draft | Cùng hub → tab **Mẫu / Designer** |
| Duyệt | **Hộp thư duyệt** |

Hai tab cùng một hub + duyệt xong đổi gì: [Cấu hình luồng duyệt](/docs/guide-approval-flows).  
Vẽ & kéo nối: [Phân quy trình duyệt](/docs/guide-workflows).

---

## Ba luồng FTECH — tạo & kích hoạt

**Tạo luồng mới** → điền bảng → tick **Đang kích hoạt** → **Tạo mới**.

| Loại | Tên luồng | Bước |
|------|-----------|------|
| **Nghỉ phép** | *Nghỉ phép — QL rồi HR* | Hùng → Mai |
| **Yêu cầu mua** | *Yêu cầu mua trên 5 triệu — Trưởng phòng rồi CFO* | Anh → Loan |
| **Bảng lương** | *Chốt bảng lương — Kế toán trưởng rồi Admin* | Loan → Admin |

Thẻ khác cùng loại = **Chưa gắn — không tự chạy**.

---

## Nghỉ phép

![Bảng đơn — *Tuấn* · Phép năm 10–12/08 · Workflow QL→HR](/docs-assets/eu/wf-leave-table-columns.png)

| Ô | FTECH demo |
|---|------------|
| NV | *Lê Minh Tuấn* |
| Loại / ngày | **Phép năm** · *10–12/08/2026* |
| Lý do | *Nghỉ phép du lịch Đà Nẵng cùng gia đình.* |

**Đơn Nghỉ Phép** → **Gửi đơn** → *Hùng* → *Mai*.

**Sau duyệt hết:** đơn **Đã duyệt** (ghi người / ngày). Từ chối → **Từ chối** + lý do.

![Ngăn chi tiết — lý do & luồng Hùng → Mai](/docs-assets/eu/leave-detail-drawer.png)

![Hộp thư — tab **Chờ tôi duyệt**](/docs-assets/eu/approval-inbox-tabs.png)

![Trạng thái sau duyệt](/docs-assets/eu/leave-status-badges.png)

---

## Yêu cầu mua

| Trường | FTECH demo |
|--------|------------|
| NCC | *HTX Rau Sạch Đà Lạt* |
| Dòng | *Rau Cải Xanh Đà Lạt* · SL *200* · ĐG *25.000* (~*5.000.000*) |
| Ghi chú | *Bổ sung canteen FTECH HN — tuần 11/08/2026* |

Tạo/gửi PR → *Anh* → *Loan* ở **Hộp thư duyệt**.

**Sau duyệt hết:** PR **Đã duyệt** + thông báo người tạo. **Chưa** nhập kho / **chưa** trừ tồn — cần PO → phiếu nhập (GRN) nếu muốn tăng tồn. Chi tiết: [Đơn hàng & tồn kho](/docs/guide-warehouse-sales).

![Hai tab **Chờ tôi duyệt** / **Tất cả**](/docs-assets/eu/approval-inbox-tabs.png)

---

## Bảng lương

![Thẻ Approval kỳ lương 07/2026 — nút Inbox](/docs-assets/eu/payroll-approval-bar.png)

1. **Bảng Lương** → kỳ **07/2026** → tính nếu cần
2. **Tạo kỳ** → **Khoá kỳ**
3. *Loan* → Admin trên **Hộp thư duyệt**

**Sau duyệt hết:** kỳ **Đã đóng**. Từ chối → kỳ mở lại để sửa.

![Tab **Chờ tôi duyệt** — kỳ 07/2026](/docs-assets/eu/approval-inbox-tabs.png)

---

## Lỗi nhanh

| Thấy | Xử lý |
|------|--------|
| Lưu Designer, đơn không chạy | Kích hoạt ở **Cấu hình luồng duyệt** |
| Hai thẻ cùng loại | Chỉ **Áp dụng: …** chạy |
| Inbox trống (mua/lương) | Chưa gửi PR / chưa **Khoá kỳ** |
| Mong hoá đơn trừ kho | Hiện **chưa** — xem [Đơn hàng & tồn kho](/docs/guide-warehouse-sales) |

→ [Cấu hình luồng duyệt](/docs/guide-approval-flows) · [Hộp thư duyệt](/docs/guide-approval-inbox) · [Xin nghỉ phép](/docs/guide-leave) · [Bảng lương](/docs/guide-payroll)
