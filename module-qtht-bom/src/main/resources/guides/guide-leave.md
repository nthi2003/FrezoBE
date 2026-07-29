# Xin nghỉ phép

**Nhân sự** → **Đơn Nghỉ Phép** (`/qlns/leaves`) — gửi đơn, theo dõi trạng thái; cấp trên duyệt ở **Hộp thư duyệt**.

**Ảnh minh hoạ:** `/docs-assets/eu/` (FTECH demo)

![Topbar — *Lê Minh Tuấn*](/docs-assets/eu/topbar.png)

![Menu — vào **Nhân sự** / nhóm liên quan](/docs-assets/eu/menu-sidebar.png)

![Màn Đơn Nghỉ Phép — danh sách + trạng thái](/docs-assets/eu/leave-list.png)

![Hai tab **Cần tôi duyệt** · **Đơn của tôi**](/docs-assets/eu/leave-tabs.png)

---

## Luồng nhân viên — xin nghỉ

*Ví dụ FTECH: **Lê Minh Tuấn** · Phòng Công Nghệ Thông Tin*

![Tab **Đơn của tôi** — đơn của bạn](/docs-assets/eu/leave-tabs.png)

1. Vào **Nhân sự** → **Đơn Nghỉ Phép**.
2. Bấm tab **Đơn của tôi** → **Tạo đơn** (góc trên bên phải).
3. Điền hộp thoại **Tạo đơn xin nghỉ phép**:

| Ô | FTECH demo |
|---|------------|
| **Nhân viên** | *Lê Minh Tuấn* |
| **Loại nghỉ** | **Phép năm** (có lương) |
| **Từ ngày** → **Đến ngày** | *10/08/2026* → *12/08/2026* |
| **Lý do** (≥ 5 ký tự) | *Nghỉ phép du lịch Đà Nẵng cùng gia đình.* |
| **File đính kèm** | Phép năm: để trống · Nghỉ ốm: dán link giấy khám (vd. BV Bạch Mai) |

Hệ thống tự đếm **ngày làm việc** (đã trừ T7–CN). Chuỗi duyệt trên form: **Bạn gửi** → **QL trực tiếp** → **HR chốt** → **Có hiệu lực**.

4. Bấm **Gửi đơn**.
5. Ở tab **Đơn của tôi**, theo dõi cột **Workflow** / **Trạng thái**.

![Bảng đơn — *Tuấn* · Phép năm 10–12/08 · Workflow QL→HR](/docs-assets/eu/wf-leave-table-columns.png)

![Ngăn chi tiết — lý do · luồng duyệt](/docs-assets/eu/leave-detail-drawer.png)

![Badge **Chờ QL** / **Chờ HR** / **Duyệt** / **Từ chối**](/docs-assets/eu/leave-status-badges.png)

**Kết quả:** Đơn **Chờ QL** — chờ *Nguyễn Văn Hùng* (QL) rồi *Trần Thị Mai* (HR).

| Trạng thái | Nghĩa |
|------------|--------|
| **Chờ QL** | QL trực tiếp chưa xử lý |
| **Chờ HR** | QL đã đồng ý, chờ HR chốt |
| **Duyệt** | Đã duyệt — lịch nghỉ có hiệu lực |
| **Từ chối** | Bị từ chối — mở đơn đọc lý do |
| **Huỷ** | Đơn đã huỷ |

Loại nghỉ thường dùng: **Phép năm**, **Nghỉ ốm**, **Không lương**, **Kết hôn** (+ Hiếu, Thai sản, Vợ sinh, Khác trên form).

---

## Luồng cấp trên — duyệt đơn

*Ví dụ: *Hùng* (QL) → *Mai* (HR) duyệt đơn *Tuấn*`*

![Hộp thư — tab **Chờ tôi duyệt**](/docs-assets/eu/approval-inbox-tabs.png)

![Cột Workflow — *Tuấn* chờ bước *Hùng*](/docs-assets/eu/wf-leave-table-columns.png)

![Ngăn chi tiết — lý do · luồng *Hùng → Mai*](/docs-assets/eu/leave-detail-drawer.png)

1. Đăng nhập tài khoản người duyệt → **Phê duyệt** → **Hộp thư duyệt** (hoặc từ ngăn chi tiết đơn bấm **Mở Inbox**).
2. Tab **Chờ tôi duyệt** → mở dòng *Lê Minh Tuấn* · Phép năm · *10–12/08/2026*.
3. Đọc lý do / ngày → **Duyệt** hoặc **Từ chối** (lý do từ chối ≥ 3 ký tự).

| Bước | Người | Ghi chú mẫu |
|------|--------|-------------|
| QL | *Nguyễn Văn Hùng* | *Đồng ý cho nghỉ theo lịch đã gửi.* |
| HR | *Trần Thị Mai* | Chốt cuối → đơn **Duyệt** |
| Từ chối | QL hoặc HR | *Trùng deadline release; nghỉ sau 15/08.* |

4. *Tuấn* mở lại **Đơn của tôi** → badge **Duyệt** hoặc **Từ chối**.

![Badge sau duyệt / từ chối](/docs-assets/eu/leave-status-badges.png)

Tab **Cần tôi duyệt** trên **Đơn Nghỉ Phép** chỉ liệt kê đơn chờ bạn — thao tác **Duyệt** / **Từ chối** chính thức ở **Hộp thư duyệt**.

→ Chi tiết inbox: [Hộp thư duyệt](/docs/guide-approval-inbox)

---

## HR / cấu hình & số dư phép (ngắn)

**Lưu ý:** Chỉ Admin (hoặc vai trò cấu hình) gắn ai duyệt nghỉ.

![Menu — **Cấu hình luồng duyệt**](/docs-assets/eu/wf-sidebar-approval.png)

- Gắn / kích hoạt luồng **Nghỉ phép — QL rồi HR** (*Hùng* → *Mai*): [Gắn luồng duyệt](/docs/guide-approval-attach).
- Loại nghỉ chọn sẵn trên form (**Phép năm**, **Nghỉ ốm**…).
- **Số dư phép:** hỏi HR trước khi gửi nếu nghi hết ngày / trùng lịch — hệ thống chặn khi khoảng ngày không hợp lệ.

---

## Lỗi thường gặp

- **Không thấy Tạo đơn** → thiếu quyền tạo đơn hoặc chưa gắn hồ sơ NV — liên hệ HR.
- **“Bạn chưa có đơn nào”** dù vừa gửi → đang ở tab **Cần tôi duyệt**; sang **Đơn của tôi**.
- **Inbox trống** → đơn còn bước trước; thử tab **Tất cả** trên hộp thư.
- **Không ai nhận đơn** → nhờ Admin kiểm tra luồng **Nghỉ phép** đang **Áp dụng**.
- **Lý do quá ngắn** → gửi đơn ≥ 5 ký tự; từ chối ≥ 3 ký tự.

→ [Hộp thư duyệt](/docs/guide-approval-inbox) · [Gắn luồng duyệt](/docs/guide-approval-attach) · [Nghỉ phép (mobile)](/docs/guide-mobile-leave)
