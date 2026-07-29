# Tính và chốt bảng lương

Để bộ phận lương chạy lương một tháng: tính cho cả kỳ, soát từng phiếu, chốt lại rồi chuyển sang chi trả.

![Màn Bảng lương — thanh chọn kỳ, các thẻ tổng và danh sách nhân viên](/docs-assets/eu/payroll-list.png)

## Làm việc chính

1. Vào menu **Nhân sự** → **Bảng Lương**.
2. Chọn **tháng** và **năm** trên thanh chọn kỳ — ví dụ **Tháng 7** / **Năm 2026**; nhãn **KỲ HIỆN TẠI** cho biết bạn đang ở tháng này. Kỳ trên danh sách thường hiện dạng *Kỳ lương 07/2026*.
3. Bấm **Tính lương kỳ này** (nút xanh, góc trên bên phải) → hộp thoại hiện ra, cho biết sẽ tính cho bao nhiêu nhân viên đủ điều kiện (công ty mẫu ~10 người có HĐ hiệu lực).
4. Đọc phần hệ thống sẽ tự làm (lương cơ bản theo hợp đồng, công thực tế, thưởng/phụ cấp, bảo hiểm và thuế) → bấm **Bắt đầu tính**.
5. Xem bốn thẻ tổng phía trên: **Bản nháp**, **Đã chốt**, **Đã thanh toán**, **Tổng chi trả kỳ này**.
6. Soát từng người: bấm icon **con mắt** (*Xem phiếu lương*) ở cuối dòng. Ví dụ số trên phiếu (theo HĐ mẫu):

| Nhân viên | Chức danh / phòng | Lương cơ bản HĐ (tham chiếu) |
|-----------|-------------------|------------------------------|
| *Nguyễn Văn Hùng* | Trưởng phòng IT | ~*45.000.000* |
| *Lê Minh Tuấn* | Kỹ sư phần mềm Senior · IT | ~*30.000.000* |
| *Bùi Thanh Loan* | Kế toán trưởng · Tài chính | ~*38.000.000* |

7. Khi số đã đúng: bấm icon **dấu tick** (*Chốt lương*) trên dòng đó → trạng thái đổi thành **Đã chốt**.

**Kết quả:** Bảng lương kỳ này hiện đủ nhân viên, mỗi dòng có trạng thái rõ ràng; những dòng đã chốt không sửa được nữa.

![Hộp thoại tính lương — nêu số nhân viên đủ điều kiện và nút Bắt đầu tính](/docs-assets/eu/payroll-calc-dialog.png)

## Đọc một phiếu lương

Ngăn kéo **Phiếu lương** mở ra từ icon con mắt, gồm:

- Ba chỉ số lớn: **Tổng thu nhập (Gross)**, **Thực nhận (Net)**, **Chi phí công ty**.
- **Cơ sở tính lương**: lương cơ bản, lương đóng bảo hiểm, ngày công chuẩn, ngày làm thực tế, nghỉ có lương / không lương, đi muộn.
- **Chi tiết thu nhập**: lương theo ngày công, phụ cấp, các khoản phạt.
- Phần bảo hiểm bắt buộc: phần người lao động đóng và phần công ty đóng.

Nút **Xuất / In** ở đáy ngăn kéo để gửi phiếu cho nhân viên.

![Ngăn kéo Phiếu lương — Gross, Net, cơ sở tính lương và chi tiết thu nhập](/docs-assets/eu/payroll-payslip-drawer.png)

## Thêm thưởng hoặc phụ cấp

1. Tìm dòng còn ở trạng thái **Bản nháp** (dòng đã chốt không sửa được) — ví dụ dòng *Lê Minh Tuấn*.
2. Bấm icon **dấu cộng tròn** (*Thêm thưởng / phụ cấp*) ở cuối dòng.
3. Điền **Số tiền (VNĐ)** *1.500.000* và **Lý do** *Thưởng KPI dự án Q2 / hoàn thành release tháng 7* → lưu lại.
4. Mở lại phiếu bằng icon con mắt để xác nhận số **Thực nhận** đã đổi.

## Gửi kỳ lương đi duyệt

Thẻ **Approval kỳ lương** nằm ngay dưới tiêu đề trang (vd. **Approval kỳ lương 07/2026**), cho biết kỳ này đã có phiếu duyệt hay chưa.

1. Nếu thẻ ghi *chưa có kỳ*: bấm **Tạo kỳ**.
2. Bấm **Khoá kỳ** để chốt số liệu và gửi sang người duyệt (luồng mẫu: **CFO / Kế toán trưởng** *Bùi Thanh Loan* → **Admin hệ thống** — xem [Gắn luồng duyệt](/docs/guide-approval-attach)).
3. Bấm **Inbox** để sang **Hộp thư duyệt** theo dõi ai đang giữ phiếu.
4. Bấm **Timeline** nếu muốn xem đã qua những bước duyệt nào.

**Lưu ý:** Chỉ tài khoản được cấp quyền quản lý kỳ lương mới thấy các nút **Tạo kỳ** / **Khoá kỳ** / **Mở khoá**. Chỉ tính lương **không** đủ để hiện phiếu ở inbox — phải **Khoá kỳ** sau khi luồng **Bảng lương** đã **Áp dụng**.

![Thẻ Approval kỳ lương với nút Inbox](/docs-assets/eu/payroll-approval-bar.png)

## Đánh dấu đã chi trả

1. Lọc tab **Đã chốt** để chỉ còn những dòng chờ chuyển khoản.
2. Sau khi ngân hàng báo có, bấm icon **bàn tay đỡ tiền** (*Đánh dấu đã thanh toán*) trên từng dòng.
3. Trạng thái đổi thành **Đã thanh toán** và dòng chuyển sang tab cùng tên.

Kế toán muốn đưa số lương sang sổ: bấm **Hạch toán → GL** ở đầu trang rồi xác nhận. Chạy lại nhiều lần cũng không bị ghi trùng.

## Lỗi thường gặp

- **Không thấy nút Tính lương kỳ này:** Tài khoản chưa được cấp quyền chạy lương — nhờ quản trị hệ thống cấp quyền hoặc nhờ bộ phận lương thao tác.
- **Ít người trong bảng hơn số nhân viên thực tế:** Chỉ nhân viên có **hợp đồng đang hiệu lực** mới được tính. Mở **Hợp Đồng Lao Động** bổ sung rồi bấm tính lại.
- **Tính lại nhưng số cũ vẫn còn:** Chạy lại chỉ ghi đè các dòng **Bản nháp**. Dòng **Đã chốt** hoặc **Đã thanh toán** phải mở khoá / huỷ trước.
- **Số công sai:** Sửa ở chấm công hoặc đơn nghỉ phép trước, rồi tính lại bảng — đừng sửa tay trên phiếu lương.
- **Nhân viên chưa thấy phiếu trên Mobile:** Kỳ chưa được chốt và chi trả; hoàn tất các bước ở trên trước.
