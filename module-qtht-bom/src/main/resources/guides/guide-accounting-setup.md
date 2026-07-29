# Kế toán — thiết lập ban đầu

Test live **FTECH HN** · 1 lần/công ty: TT → Seed COA → năm *2026* → khóa/mở kỳ.

![Topbar FTECH](/docs-assets/eu/topbar.png)

![Menu Kế toán](/docs-assets/eu/acc-sidebar.png)

![Cài đặt TT133 · VND](/docs-assets/eu/acc-settings.png)

---

## Data test FTECH

![Chọn tổ chức FTECH HN](/docs-assets/eu/settings-org-dropdown.png)

| Mục | Giá trị gõ / chọn |
|-----|-------------------|
| Tổ chức | *FTECH HN* · code *FTECH_HN* |
| Tên CT (đối chiếu HĐ) | *Tổng Công ty Công nghệ FTECH* |
| MST | *0101234567* |
| Địa chỉ | *Tầng 10, FTECH Tower, Số 1 Phạm Văn Bạch, Cầu Giấy, Hà Nội* |
| User | Admin hoặc *Bùi Thanh Loan* (KT trưởng) |
| Quyền cần | `ACCOUNTING.SETTING.UPDATE` · `ACCOUNTS.CREATE` · `PERIODS.CREATE` / khóa kỳ |
| Menu | **Kế toán** → Cài đặt · Hệ thống TK · Kỳ kế toán |
| Route | `/accounting/settings` · `/accounting/accounts` · `/accounting/periods` |

MST nằm ở hồ sơ CT — màn Cài đặt KT chỉ có TT · tiền tệ · mapping TK.

---

## B1 — Cài đặt + Seed COA

![Form cấu hình](/docs-assets/eu/acc-settings.png)

![COA trống / sau seed](/docs-assets/eu/acc-coa-list.png)

1. Vào **Kế toán** → **Cài đặt kế toán** (`/accounting/settings`)
2. Chọn đúng bảng dưới → **Lưu + Seed COA theo chuẩn**

| Ô | Giá trị test |
|---|--------------|
| Thông tư áp dụng | *TT 133/2016 — Doanh nghiệp nhỏ và vừa* |
| Tiền tệ | *VND* |
| Chiến lược hạch toán lương | *1 bút toán tổng hợp / kỳ (Recommended)* |
| TK chi phí lương (Nợ) | *6421* |
| TK phải trả CBCNV (Có) | *334* |
| TK BHXH · BHYT · BHTN | *3383* · *3384* · *3385* |
| TK thuế TNCN · KPCĐ | *3335* · *3382* |

Hoặc: **Lưu cấu hình** → **Hệ thống tài khoản** → **Seed COA**. Seed chỉ thêm TK chưa có.

---

## B2 — Kiểm tra COA + TK lẻ

![List Hệ thống tài khoản](/docs-assets/eu/acc-coa-list.png)

![Sidebar — Hệ thống tài khoản](/docs-assets/eu/acc-sidebar.png)

1. **Kế toán** → **Hệ thống tài khoản** (`/accounting/accounts`)
2. Tìm *334* · *6421* · *1111* → phải thấy tên chuẩn TT133; **Tổng số TK** > 0
3. Thêm TK quỹ VP (chưa có sau Seed):
   - **Thêm TK** → số *1113* · tên *Tiền mặt VND — quỹ văn phòng FTECH HN* · loại **Tài sản** · cha *111* · chuẩn *TT133* → Lưu

---

## B3 — Tạo năm 2026 + 12 kỳ

![Nút Kỳ kế toán trên Cài đặt](/docs-assets/eu/acc-settings.png)

![Chứng từ — kỳ 2026 / T7](/docs-assets/eu/acc-journals-list.png)

1. Trên `/accounting/settings` bấm **Tạo năm tài chính 2026 + 12 kỳ**  
   *hoặc* **Kỳ kế toán** (`/accounting/periods`) → Năm *2026* → **Tạo năm 2026 + 12 kỳ**
2. List: **Tháng 1/2026** … **Tháng 12/2026**

| Kỳ dùng test | Từ → Đến | Trạng thái |
|--------------|----------|------------|
| *Tháng 7/2026* | *2026-07-01* → *2026-07-31* | giữ **Mở** (lương / CT) |
| *Tháng 6/2026* | *2026-06-01* → *2026-06-30* | dùng luyện **Khóa kỳ** |

---

## B4 — Khóa kỳ / mở lại

![Menu Kỳ kế toán](/docs-assets/eu/acc-sidebar.png)

![Chứng từ — chỉ ghi khi kỳ Mở](/docs-assets/eu/acc-journals-list.png)

![Sổ cái sau khóa](/docs-assets/eu/acc-ledger.png)

1. **Kế toán** → **Kỳ kế toán** (`/accounting/periods`) · năm *2026*
2. Dòng *Tháng 6* → **Khóa kỳ** → xác nhận → badge **Đã khóa**
3. Cần sửa → **Mở lại**. **Khóa cứng** = không mở từ UI.

| Trạng thái | Được làm |
|------------|----------|
| **Mở** | Ghi sổ CT · **Khóa kỳ** |
| **Đã khóa** | **Mở lại** |
| **Khóa cứng** | Không mở UI |

Giữ *Tháng 7/2026* **Mở** cho các guide kỳ *07/2026*.

---

## Lỗi thường gặp

![Hub tài liệu](/docs-assets/eu/docs-hub-list.png)

| Hiện tượng | Cách xử lý |
|------------|------------|
| Không thấy menu / **Lưu** / **Seed** | Thiếu quyền — nhờ Admin |
| **Tổng số TK: 0** | Bấm **Lưu + Seed COA** (hoặc **Seed COA** trên màn TK) |
| *Số hiệu đã tồn tại* | Đổi số (vd *1113*), tránh trùng *1111* / *1112* |
| Không ghi sổ *06/2026* | Kỳ **Đã khóa** → **Mở lại** hoặc dùng *07/2026* |

→ [Chứng từ và ghi sổ](/docs/guide-accounting-journal) · [Đi hết một kỳ](/docs/guide-accounting)
