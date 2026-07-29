# Quy trình duyệt — ba chỗ, đừng lẫn

Frezo tách **ba việc**: vẽ mẫu → bật cho loại đơn → duyệt hàng ngày. Nhầm chỗ là lý do hay thấy “đã lưu mà đơn không chạy”.

**Ảnh minh hoạ:** `/docs-assets/eu/` (FTECH demo)

![Menu — Quy Trình Duyệt vs Cấu hình luồng duyệt](/docs-assets/eu/wf-sidebar-approval.png)

---

## Ba chỗ — nói tiếng thường

| Việc bạn muốn | Vào đâu | Giống như… |
|---------------|---------|------------|
| **Vẽ** chuỗi bước (ai duyệt trước, ai sau) | **Quy Trình Duyệt** → **Designer** (`/qtht/workflows`) | Vẽ bản thảo trên giấy |
| **Bật** bản thảo cho nghỉ / mua / lương | **Cấu hình luồng duyệt** (`/approval/flows`) | Dán bản thảo lên tường công ty — đơn mới mới theo |
| **Duyệt** đơn đang chờ mình | **Phê duyệt** → **Hộp thư duyệt** (`/approval/inbox`) | Xử lý việc hôm nay |

> **Designer ≠ Hộp thư.** Designer không duyệt đơn. Hộp thư không vẽ sơ đồ.

![Đầu trang Designer](/docs-assets/eu/cta-wf-designer-header.png)

---

## Làm việc chính — từ mẫu đến đơn chạy

1. **Quản trị hệ thống** → **Quy Trình Duyệt** (hoặc **Thư viện mẫu** → **Clone & Designer**).
2. Đặt tên dễ hiểu, ví dụ *Nghỉ phép — QL rồi HR (bản thiết kế)*. Chọn đúng loại việc.
3. Kéo nối: **Bắt đầu** → **Duyệt** (QL) → **Duyệt** (HR) → **Kết thúc** → **Lưu graph** → **Kiểm tra**.
4. Sang **Cấu hình luồng duyệt** → tạo/sửa thẻ cùng loại → tick **Đang kích hoạt** → lưu đến khi thấy badge **Áp dụng: …**.
5. Gửi đơn thật (ví dụ nghỉ *Tuấn*) → người duyệt mở **Hộp thư duyệt** → **Duyệt** / **Từ chối**.

**Kết quả:** Đơn mới đi đúng chuỗi; badge trạng thái trên đơn đổi sau khi duyệt hết.

![Cấu hình luồng — **Tạo luồng mới**](/docs-assets/eu/cta-wf-flows-header.png)

![Hộp thư — **Chờ tôi duyệt**](/docs-assets/eu/approval-inbox-tabs.png)

---

## Designer (Quy Trình Duyệt) — chỉ là bản vẽ

**Lưu ý:** Chỉ Admin / vai trò cấu hình mới thấy menu này.

- Dùng để **thiết kế** và lưu mẫu; có thể Copy mẫu gần giống rồi sửa nhẹ.
- Nhãn bước viết tiếng người: *Quản lý trực tiếp duyệt*, *HR chốt phép* — tránh `node_2`, `test`.
- Thẻ ghi **Chưa gắn — không tự chạy** = mới là bản vẽ, **chưa** điều khiển đơn.

Vai trò FTECH hay dùng: **Hùng** QL · **Mai** HR · **Loan** KT trưởng · **Anh** Trưởng phòng.

---

## Cấu hình gắn module — chỗ đơn thật chạy

Trang riêng: [Cấu hình luồng duyệt](/docs/guide-approval-flows).

| Loại | Tên luồng (FTECH) | Ai duyệt |
|------|-------------------|----------|
| Nghỉ phép | *Nghỉ phép — QL rồi HR* | Hùng → Mai |
| Yêu cầu mua | *Yêu cầu mua trên 5 triệu — TP rồi CFO* | Anh → Loan |
| Bảng lương | *Chốt bảng lương — KT trưởng rồi Admin* | Loan → Admin |

Hai thẻ cùng loại chỉ **một** thẻ **Áp dụng** chạy.

---

## Hộp thư duyệt — việc hàng ngày

Không mở Designer để duyệt. Vào **Phê duyệt** → **Hộp thư duyệt** → tab **Chờ tôi duyệt**.

Sau khi duyệt hết, nghiệp vụ đổi gì (nghỉ / mua / lương): xem bảng trong [Cấu hình luồng duyệt](/docs/guide-approval-flows#duyệt-xong-thì-nghiệp-vụ-đổi-gì).

![Badge sau duyệt](/docs-assets/eu/leave-status-badges.png)

---

## Kiểm tra nhanh

- [ ] Đã **Lưu graph** ở Designer
- [ ] Thẻ có badge **Áp dụng:** đúng loại ở Cấu hình luồng duyệt
- [ ] Gửi thử đơn → thấy ở Hộp thư của đúng người
- [ ] Duyệt ở Hộp thư — không nhầm Designer

## Lỗi thường gặp

| Thấy | Nguyên nhân |
|------|-------------|
| Lưu graph mà đơn vẫn cũ | Chưa kích hoạt ở **Cấu hình luồng duyệt** |
| Mở Designer để duyệt | Sai chỗ → dùng **Hộp thư duyệt** |
| Hai thẻ cùng loại | Chỉ thẻ **Áp dụng** chạy |
| Inbox trống | Chưa gửi đơn / chưa khóa kỳ lương / sai người bước hiện tại |

→ [Cấu hình luồng duyệt](/docs/guide-approval-flows) · [Gắn 3 luồng FTECH](/docs/guide-approval-attach) · [Hộp thư duyệt](/docs/guide-approval-inbox)
