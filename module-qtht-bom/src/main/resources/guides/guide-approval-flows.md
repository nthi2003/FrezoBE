# Cấu hình luồng duyệt — đơn đi theo ai?

Trang này **bật** chuỗi người duyệt cho từng loại đơn (nghỉ phép, mua hàng, lương…). Chỉ Admin / người được cấp quyền cấu hình.

Hub **một chỗ duy nhất** (`/approval/flows`):

- Tab **Luồng đang chạy** — gắn & kích hoạt (đơn thật chạy theo đây)
- Tab **Mẫu / Designer** — vẽ mẫu sơ đồ nâng cao (không tự gắn Leave)

**Ảnh minh hoạ:** `/docs-assets/eu/` (FTECH demo)

![Đầu trang Cấu hình luồng duyệt — **Tạo luồng mới**](/docs-assets/eu/cta-wf-flows-header.png)

---

## Làm việc chính

1. Menu **Quản trị hệ thống** → **Cấu hình luồng duyệt** (`/approval/flows`) → tab **Luồng đang chạy**.
2. Bấm **Tạo luồng mới** (hoặc **Sửa** thẻ sẵn có).
3. Chọn **Loại đối tượng** (Nghỉ phép / Yêu cầu mua / Bảng lương…) → thêm bước → chọn vai trò duyệt.
4. Tick **Đang kích hoạt** → **Tạo mới** / lưu.
5. Badge **Áp dụng: …** hiện trên thẻ → gửi đơn thật để kiểm chứng ở **Hộp thư duyệt**.

**Kết quả:** Đơn mới đúng loại đi theo đúng chuỗi người trên thẻ đang kích hoạt.

![Menu — Cấu hình luồng duyệt](/docs-assets/eu/wf-sidebar-approval.png)

---

## Hai tab trong cùng một hub

| Tab | Việc | Đơn thật chạy? |
|-----|------|----------------|
| **Luồng đang chạy** (`/approval/flows`) | Gắn & bật chuỗi duyệt theo loại đơn | **Có** |
| **Mẫu / Designer** (`/approval/flows?tab=templates`) | Vẽ / lưu **mẫu** sơ đồ (Designer) | **Không** (với nghỉ / mua / lương) |
| **Hộp thư duyệt** (`/approval/inbox`) | Duyệt / từ chối đơn đang chờ bạn | — |

Hai việc khác nhau: **vẽ mẫu** ≠ **bật cho đơn chạy**. Cùng một menu nên không còn hai mục sidebar giống nhau.

> Chỉ thẻ có badge **Áp dụng: …** mới điều khiển đơn mới. Designer chỉ là bản vẽ.

Chi tiết vẽ mẫu: [Phân quy trình duyệt](/docs/guide-workflows).  
Cách gắn 3 luồng FTECH: [Gắn luồng duyệt](/docs/guide-approval-attach).

---

## Duyệt xong thì nghiệp vụ đổi gì?

Khi người cuối cùng bấm **Duyệt** ở **Hộp thư duyệt**, hệ thống cập nhật **đúng loại đơn** đó — không tự làm hết mọi việc liên quan.

| Loại đơn | Sau khi **duyệt hết** | Sau khi **từ chối** | Không tự làm |
|----------|----------------------|---------------------|--------------|
| **Nghỉ phép** | Đơn → **Đã duyệt**; ghi người / ngày duyệt | Đơn → **Từ chối** + lý do | Không tự tạo lịch công khác ngoài trạng thái đơn |
| **Yêu cầu mua (PR)** | PR → **Đã duyệt**; cảnh báo tồn (nếu gắn) → đã xử lý; người tạo nhận thông báo | PR → **Từ chối**; bỏ gắn cảnh báo | **Không** tự tạo đơn mua (PO), **không** nhập kho, **không** trừ tồn |
| **Bảng lương** | Kỳ lương → **Đã đóng** (khóa) | Kỳ mở lại; bỏ khóa / bỏ phiếu duyệt | Không tự chi tiền / không tự ghi sổ kế toán |
| **Hoá đơn** (nếu có luồng) | Theo trạng thái phiếu duyệt | — | **Xuất hoá đơn không trừ kho** — xem [Đơn hàng, hoá đơn & tồn kho](/docs/guide-warehouse-sales) |

![Badge trạng thái sau duyệt — nghỉ phép](/docs-assets/eu/leave-status-badges.png)

![Hộp thư — tab **Chờ tôi duyệt**](/docs-assets/eu/approval-inbox-tabs.png)

### Ví dụ FTECH — nghỉ phép

*Tuấn* gửi Phép năm 10–12/08 → *Hùng* duyệt → *Mai* duyệt hết → đơn **Đã duyệt**. Nhân sự thấy badge đổi; không cần mở Designer.

![Bảng đơn — cột Workflow QL→HR](/docs-assets/eu/wf-leave-table-columns.png)

### Ví dụ — yêu cầu mua

PR *HTX Rau Sạch Đà Lạt* ~5.000.000 được duyệt hết → trạng thái PR **Đã duyệt**. Muốn hàng vào kho: tạo **Đơn mua** → **Phiếu nhập (GRN)** rồi **Xác nhận** — đó là bước kho riêng.

### Ví dụ — bảng lương

Khóa kỳ **07/2026** → *Loan* rồi Admin duyệt hết → kỳ **Đã đóng**. Từ chối → kỳ mở lại để sửa số liệu.

![Thanh Approval kỳ lương](/docs-assets/eu/payroll-approval-bar.png)

---

## Lỗi thường gặp

| Thấy | Xử lý |
|------|--------|
| Lưu ở Designer, đơn vẫn người cũ | Phải **kích hoạt** ở tab Luồng đang chạy |
| Hai thẻ cùng loại | Chỉ thẻ **Áp dụng: …** chạy |
| Inbox trống sau khi gửi | Chưa tick kích hoạt / sai loại đối tượng / chưa gửi hoặc chưa khóa kỳ lương |
| “Không có người duyệt” | Gán đúng vai trò (QL, HR…) cho user thật |

→ [Hộp thư duyệt](/docs/guide-approval-inbox) · [Gắn 3 luồng FTECH](/docs/guide-approval-attach) · [Đơn hàng & tồn kho](/docs/guide-warehouse-sales)
