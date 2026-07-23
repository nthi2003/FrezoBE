-- =====================================================================
-- Hotfix: password plain / thiếu {bcrypt} → DelegatingPasswordEncoder null id
-- =====================================================================
-- Root cause thường gặp:
--   1) users.password còn varchar(50) → không chứa nổi {bcrypt}$2a$10$... (~68 chars)
--   2) DataInitializer rehash FAIL (value too long) → password plain "123456" còn nguyên
--   3) Login: There is no PasswordEncoder mapped for the id "null"
--
-- Chạy trên DB native (Frezo local): D:\PostgreSQL\17  /  database frezo
-- Sau khi chạy: login admin / 123456 — KHÔNG cần restart BE nếu password đã đúng format.
-- =====================================================================

BEGIN;

ALTER TABLE users ALTER COLUMN password TYPE varchar(255);

-- Hash dưới đây = DelegatingPasswordEncoder.encode("123456")
-- (bcrypt strength 10). Mỗi lần generate ra salt khác — hash này đã verify matches=true.
UPDATE users
SET password = '{bcrypt}$2a$10$BchQWK4ymrQLvEc8mqj97.nImCjBYnjYVng/fkZK1P77dg4iaZ23.',
    updated_date = NOW(),
    updated_by = 'system'
WHERE password IS NOT NULL
  AND password NOT LIKE '{bcrypt}%';

COMMIT;

-- Verify
SELECT user_name, LEFT(password, 20) AS prefix, LENGTH(password) AS len
FROM users
WHERE user_name IN ('admin', 'superadmin')
   OR password LIKE '{bcrypt}%'
ORDER BY user_name;
