-- =====================================================================
-- Migration: convert plain text password → DelegatingPasswordEncoder format
-- =====================================================================
-- v1.1 — Batch B (Security P0)
--
-- Bối cảnh: trước v1.1, hệ thống dùng NoOpPasswordEncoder → password lưu plain text.
-- Từ v1.1, SecurityConfig dùng DelegatingPasswordEncoder — format:
--   {bcrypt}$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxx    (khuyến nghị, tạo mới)
--   {noop}plaintext                                   (legacy, giữ tạm để không kick user)
--
-- Có 2 giai đoạn migration:
-- =====================================================================
--   PHASE 1 (RUN NGAY khi deploy v1.1) — thêm prefix {noop} cho user cũ
-- =====================================================================
-- User cũ có password plain text → không đăng nhập được vì DelegatingPasswordEncoder
-- không hiểu format không prefix. Phase 1 thêm prefix {noop} → user login tiếp được
-- (tạm thời — không hash lại vì không có plain text sau khi hash).
--
-- Chạy sau khi deploy code v1.1, TRƯỚC khi user đầu tiên login:

BEGIN;

-- Chỉ update user có password chưa có prefix (bỏ qua nếu đã migrate)
UPDATE users
SET password = '{noop}' || password
WHERE password IS NOT NULL
  AND password NOT LIKE '{%}%';

-- Verify: đếm số user đã migrate
SELECT COUNT(*) AS migrated_users FROM users WHERE password LIKE '{noop}%';

COMMIT;

-- =====================================================================
--   PHASE 2 (Chạy sau) — buộc user reset password → hash BCrypt
-- =====================================================================
-- Sau phase 1, user cũ vẫn login được với plain text. Cần chiến lược để dần
-- chuyển sang BCrypt hoàn toàn. Chọn 1 trong 3 cách:
--
--   Cách A (khuyến nghị): Buộc user đổi password lần login tiếp theo
--     - Thêm cột `must_change_password BOOLEAN DEFAULT FALSE` vào `users`
--     - Set = TRUE cho mọi user có password prefix {noop}
--     - Login flow check: nếu must_change_password = true → redirect page đổi password
--     - Sau khi đổi, mật khẩu mới sẽ auto-hash BCrypt (do DelegatingPasswordEncoder default)
--       → prefix thành {bcrypt}$2a$...
--     - Set must_change_password = FALSE
--
--   Cách B: Reset toàn bộ password → email token khôi phục
--     - Bulk update: set password = NULL, reset_key = random UUID, gửi email
--     - Tất cả user phải click link → tạo mật khẩu mới (BCrypt-hash)
--
--   Cách C: Silent migration khi login thành công
--     - Trong AuthServiceImpl.login() sau BadCredentialsException success:
--       + Check nếu user.password.startsWith("{noop}") → rehash bằng
--         passwordEncoder.encode(rawPassword) → save
--     - Ưu điểm: user không cảm nhận. Nhược: phải sửa code login.
--
-- =====================================================================
--   Verification (sau migration hoàn tất)
-- =====================================================================
-- Đếm số user còn ở dạng plain text (mục tiêu: 0):
SELECT COUNT(*) AS remaining_plaintext_users
FROM users
WHERE password LIKE '{noop}%';

-- Đếm số user đã BCrypt (mục tiêu: bằng total users):
SELECT COUNT(*) AS bcrypt_users FROM users WHERE password LIKE '{bcrypt}%';

-- =====================================================================
--   ROLLBACK — chỉ dùng khi phát hiện lỗi phase 1
-- =====================================================================
-- BEGIN;
-- UPDATE users
-- SET password = REPLACE(password, '{noop}', '')
-- WHERE password LIKE '{noop}%';
-- COMMIT;

-- =====================================================================
--   SAU KHI HOÀN TẤT MIGRATION (100% user BCrypt)
-- =====================================================================
-- 1. Edit SecurityConfig.passwordEncoder() → return new BCryptPasswordEncoder(12);
--    (thay vì PasswordEncoderFactories.createDelegatingPasswordEncoder())
-- 2. Xóa mọi hàm hỗ trợ {noop} nếu có.
-- 3. Chạy script tìm user còn {noop} lần cuối → xóa (kick force reset).
