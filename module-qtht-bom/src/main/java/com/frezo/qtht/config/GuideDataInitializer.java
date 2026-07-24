package com.frezo.qtht.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Seed / sync EU guides vào bảng {@code guide} (FR-DOC-03/04).
 * <p>
 * Idempotent: insert nếu thiếu slug; refresh body/meta nếu bản ghi vẫn do seed
 * ({@code created_by}/{@code updated_by} ∈ guide-seed|system|null) —
 * không ghi đè bài Admin đã sửa.
 */
@Slf4j
@Component
@Order(55)
@RequiredArgsConstructor
public class GuideDataInitializer implements ApplicationRunner {

    private static final String SEED_USER = "guide-seed";

    private final JdbcTemplate jdbcTemplate;

    private record Seed(
            String slug,
            String title,
            String module,
            String summary,
            int sortOrder,
            String resource
    ) {}

    private static final List<Seed> SEEDS = List.of(
            new Seed("guide-payroll", "Bảng lương", "Nhân sự",
                    "Tính lương kỳ này, xem người bị bỏ qua, rồi gửi duyệt / chi trả.", 3,
                    "guides/guide-payroll.md"),
            new Seed("guide-leave", "Xin nghỉ phép", "Nhân sự",
                    "Tạo đơn nghỉ, theo dõi duyệt trên web hoặc app Mobile.", 4,
                    "guides/guide-leave.md"),
            new Seed("guide-approval-inbox", "Hộp thư duyệt", "Duyệt",
                    "Duyệt hoặc từ chối đơn đang chờ — từng đơn hoặc hàng loạt.", 5,
                    "guides/guide-approval-inbox.md"),
            new Seed("guide-attendance-settings", "Chấm công GPS / WiFi", "Nhân sự",
                    "Admin đặt vị trí văn phòng và WiFi để Mobile check-in đúng chỗ.", 6,
                    "guides/guide-attendance-settings.md"),
            new Seed("guide-hire", "Tuyển dụng & nhận việc", "Nhân sự",
                    "Từ ứng viên trúng tuyển đến hồ sơ Person và checklist onboarding.", 7,
                    "guides/guide-hire.md"),
            new Seed("guide-workflows", "Quy trình duyệt", "Duyệt",
                    "Chọn đúng chỗ: Hộp thư duyệt, Cấu hình luồng duyệt, hay Thiết kế quy trình.", 8,
                    "guides/guide-workflows.md"),
            new Seed("guide-approval-attach", "Gắn luồng duyệt vào nghỉ phép", "Duyệt",
                    "Admin kích hoạt luồng Nghỉ phép, xem badge Áp dụng, rồi kiểm chứng ở Hộp thư duyệt.", 9,
                    "guides/guide-approval-attach.md"),
            new Seed("guide-qlts", "Quản lý tài sản", "Tài sản",
                    "Thêm tài sản, cấp phát, duyệt yêu cầu rồi bàn giao cho nhân viên.", 10,
                    "guides/guide-qlts.md"),
            new Seed("guide-asset-assign", "Yêu cầu cấp phát tài sản", "Tài sản",
                    "Gửi yêu cầu từ tab Tài sản → Cấp phát; duyệt ở tab Yêu cầu hoặc Hộp thư duyệt.", 11,
                    "guides/guide-asset-assign.md"),
            new Seed("guide-depreciation", "Khấu hao tài sản", "Tài sản",
                    "Sinh lịch trên tài sản, xem trước tháng và ghi sổ khấu hao.", 12,
                    "guides/guide-depreciation.md")
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!tableExists()) {
            log.warn("[guide-seed] table guide missing — skip (chạy migration FR-DOC-03 trước)");
            return;
        }

        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        for (Seed seed : SEEDS) {
            try {
                String body = readClasspath(seed.resource());
                if (body == null || body.isBlank()) {
                    log.warn("[guide-seed] empty body for {}", seed.slug());
                    skipped++;
                    continue;
                }

                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        """
                        SELECT id, created_by, updated_by, published
                        FROM guide
                        WHERE slug = ? AND (is_deleted = false OR is_deleted IS NULL)
                        LIMIT 1
                        """,
                        seed.slug());

                if (rows.isEmpty()) {
                    jdbcTemplate.update(
                            """
                            INSERT INTO guide (
                                id, slug, title, body, module, summary, sort_order, published,
                                created_by, created_date, updated_by, updated_date, is_deleted
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, true, ?, NOW(), ?, NOW(), false)
                            """,
                            UUID.randomUUID().toString(),
                            seed.slug(),
                            seed.title(),
                            body,
                            seed.module(),
                            seed.summary(),
                            seed.sortOrder(),
                            SEED_USER,
                            SEED_USER);
                    inserted++;
                    continue;
                }

                Map<String, Object> row = rows.get(0);
                if (!isSeedOwned(asString(row.get("created_by")), asString(row.get("updated_by")))) {
                    skipped++;
                    continue;
                }

                int n = jdbcTemplate.update(
                        """
                        UPDATE guide
                        SET title = ?, body = ?, module = ?, summary = ?, sort_order = ?,
                            published = true, updated_by = ?, updated_date = NOW(), is_deleted = false
                        WHERE id = ?
                        """,
                        seed.title(),
                        body,
                        seed.module(),
                        seed.summary(),
                        seed.sortOrder(),
                        SEED_USER,
                        asString(row.get("id")));
                if (n > 0) {
                    updated++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                log.error("[guide-seed] failed for {}: {}", seed.slug(), e.getMessage());
                skipped++;
            }
        }

        log.info("[guide-seed] done — inserted={}, updated={}, skipped={}", inserted, updated, skipped);
    }

    private boolean tableExists() {
        try {
            Integer n = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM information_schema.tables
                    WHERE table_schema = 'public' AND table_name = 'guide'
                    """,
                    Integer.class);
            return n != null && n > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isSeedOwned(String createdBy, String updatedBy) {
        String u = normalizeOwner(updatedBy);
        String c = normalizeOwner(createdBy);
        if (u != null) {
            return "guide-seed".equals(u) || "system".equals(u);
        }
        return c == null || "guide-seed".equals(c) || "system".equals(c);
    }

    private static String normalizeOwner(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static String readClasspath(String path) throws IOException {
        ClassPathResource res = new ClassPathResource(path);
        if (!res.exists()) {
            return null;
        }
        try (var in = res.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }
}
