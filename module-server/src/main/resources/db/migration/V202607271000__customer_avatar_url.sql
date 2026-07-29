-- Customer avatar: cột avatar_url + backfill demo KH001–KH010 (pravatar, đủ ảnh)

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(1000);

-- Demo customers: gán avatar cố định theo mã (idempotent — chỉ khi đang trống)
UPDATE customers SET avatar_url = 'https://i.pravatar.cc/150?img=1',  updated_date = NOW()
 WHERE code = 'KH001' AND (avatar_url IS NULL OR avatar_url = '');
UPDATE customers SET avatar_url = 'https://i.pravatar.cc/150?img=5',  updated_date = NOW()
 WHERE code = 'KH002' AND (avatar_url IS NULL OR avatar_url = '');
UPDATE customers SET avatar_url = 'https://i.pravatar.cc/150?img=12', updated_date = NOW()
 WHERE code = 'KH003' AND (avatar_url IS NULL OR avatar_url = '');
UPDATE customers SET avatar_url = 'https://i.pravatar.cc/150?img=20', updated_date = NOW()
 WHERE code = 'KH004' AND (avatar_url IS NULL OR avatar_url = '');
UPDATE customers SET avatar_url = 'https://i.pravatar.cc/150?img=33', updated_date = NOW()
 WHERE code = 'KH005' AND (avatar_url IS NULL OR avatar_url = '');
UPDATE customers SET avatar_url = 'https://i.pravatar.cc/150?img=14', updated_date = NOW()
 WHERE code = 'KH006' AND (avatar_url IS NULL OR avatar_url = '');
UPDATE customers SET avatar_url = 'https://i.pravatar.cc/150?img=25', updated_date = NOW()
 WHERE code = 'KH007' AND (avatar_url IS NULL OR avatar_url = '');
UPDATE customers SET avatar_url = 'https://i.pravatar.cc/150?img=47', updated_date = NOW()
 WHERE code = 'KH008' AND (avatar_url IS NULL OR avatar_url = '');
UPDATE customers SET avatar_url = 'https://i.pravatar.cc/150?img=44', updated_date = NOW()
 WHERE code = 'KH009' AND (avatar_url IS NULL OR avatar_url = '');
UPDATE customers SET avatar_url = 'https://i.pravatar.cc/150?img=52', updated_date = NOW()
 WHERE code = 'KH010' AND (avatar_url IS NULL OR avatar_url = '');
