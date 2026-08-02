# Quy trình duyệt — cấu hình một chỗ, duyệt chỗ khác

Frezo gom **cấu hình** vào một hub; **duyệt hàng ngày** vẫn ở Hộp thư. Nhầm chỗ là lý do hay thấy “đã lưu mà đơn không chạy”.

**Ảnh minh hoạ:** `/docs-assets/eu/` (FTECH demo)

![Menu — Cấu hình luồng duyệt](/docs-assets/eu/wf-sidebar-approval.png)

---

## Hai chỗ — nói tiếng thường

| Việc bạn muốn | Vào đâu | Giống như… |
|---------------|---------|------------|
| **Bật** chuỗi duyệt cho nghỉ / mua / lương | **Cấu hình luồng duyệt** → tab **Luồng đang chạy** (`/approval/flows`) | Dán bản thảo lên tường công ty — đơn mới mới theo |
| **Vẽ** mẫu sơ đồ nâng cao (Designer) | Cùng hub → tab **Mẫu / Designer** (`/approval/flows?tab=templates`) | Vẽ bản thảo trên giấy |
| **Duyệt** đơn đang chờ mình | **Phê duyệt** → **Hộp thư duyệt** (`/approval/inbox`) | Xử lý việc hôm nay |

> **Designer ≠ Hộp thư.** Tab Mẫu không duyệt đơn. Hộp thư không vẽ sơ đồ.
> Menu cũ `/qtht/workflows` chuyển thẳng vào tab **Mẫu / Designer**.

![Đầu trang Designer](/docs-assets/eu/cta-wf-designer-header.png)

---

## Làm việc chính — từ mẫu đến đơn chạy

1. **Quản trị hệ thống** → **Cấu hình luồng duyệt** → tab **Mẫu / Designer** (hoặc **Thư viện mẫu** → **Clone & Designer**).
2. Đặt tên dễ hiểu, ví dụ *Nghỉ phép — QL rồi HR (bản thiết kế)*. Chọn đúng loại việc.
3. Kéo nối: **Bắt đầu** → **Duyệt** (QL) → **Duyệt** (HR) → **Kết thúc** → **Lưu graph** → **Kiểm tra**.
4. Sang tab **Luồng đang chạy** → tạo/sửa thẻ cùng loại → tick **Đang kích hoạt** → lưu đến khi thấy badge **Áp dụng: …**.
5. Gửi đơn thật (ví dụ nghỉ *Tuấn*) → người duyệt mở **Hộp thư duyệt** → **Duyệt** / **Từ chối**.

**Kết quả:** Đơn mới đi đúng chuỗi; badge trạng thái trên đơn đổi sau khi duyệt hết.

![Cấu hình luồng — **Tạo luồng mới**](/docs-assets/eu/cta-wf-flows-header.png)

![Hộp thư — **Chờ tôi duyệt**](/docs-assets/eu/approval-inbox-tabs.png)

---

## Tab Mẫu / Designer — chỉ là bản vẽ

**Lưu ý:** Chỉ Admin / vai trò cấu hình mới thấy hub này.

- Dùng để **thiết kế** và lưu mẫu; có thể Copy mẫu gần giống rồi sửa nhẹ.
- Nhãn bước viết tiếng người: *Quản lý trực tiếp duyệt*, *HR chốt phép* — tránh `node_2`, `test`.
- Thẻ ghi **Chưa gắn — không tự chạy** = mới là bản vẽ, **chưa** điều khiển đơn nghỉ / mua / lương.

Vai trò FTECH hay dùng: **Hùng** QL · **Mai** HR · **Loan** KT trưởng · **Anh** Trưởng phòng.

---

## Tab Luồng đang chạy — chỗ đơn thật chạy

Chi tiết: [Cấu hình luồng duyệt](/docs/guide-approval-flows).

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

- [ ] Đã **Lưu graph** ở tab Mẫu / Designer (nếu dùng Designer)
- [ ] Thẻ có badge **Áp dụng:** đúng loại ở tab Luồng đang chạy
- [ ] Gửi thử đơn → thấy ở Hộp thư của đúng người
- [ ] Duyệt ở Hộp thư — không nhầm Designer

## Lỗi thường gặp

| Thấy | Nguyên nhân |
|------|-------------|
| Lưu graph mà đơn vẫn cũ | Chưa kích hoạt ở tab **Luồng đang chạy** |
| Mở Designer để duyệt | Sai chỗ → dùng **Hộp thư duyệt** |
| Hai thẻ cùng loại | Chỉ thẻ **Áp dụng** chạy |
| Inbox trống | Chưa gửi đơn / chưa khóa kỳ lương / sai người bước hiện tại |

→ [Cấu hình luồng duyệt](/docs/guide-approval-flows) · [Gắn 3 luồng FTECH](/docs/guide-approval-attach) · [Hộp thư duyệt](/docs/guide-approval-inbox)
