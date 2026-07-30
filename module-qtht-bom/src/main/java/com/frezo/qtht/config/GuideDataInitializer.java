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
 * Nguồn markdown: {@code classpath:guides/*.md} — đồng bộ từ FE
 * {@code packages/erp/src/docs/} (BA viết docs ở FE, copy vào đây rồi restart BE).
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

    /** Khớp EU_DOCS trong FrezoFE {@code packages/erp/src/docs/index.ts}. */
    private static final List<Seed> SEEDS = List.of(
            new Seed("getting-started", "Bắt đầu", "Chung",
                    "Đăng nhập, tìm trang và biết hôm nay làm việc gì trước.", 1,
                    "guides/getting-started.md"),
            new Seed("menu-guide", "Hướng dẫn menu", "Chung",
                    "Menu bên trái nghĩa là gì — mở nhóm, tìm trang, mở Tài liệu.", 2,
                    "guides/menu-guide.md"),
            new Seed("guide-payroll", "Bảng lương", "Nhân sự",
                    "Tính lương cả kỳ, soát từng phiếu, chốt rồi đánh dấu đã chi trả.", 3,
                    "guides/guide-payroll.md"),
            new Seed("guide-leave", "Xin nghỉ phép", "Nhân sự",
                    "Tạo đơn nghỉ với data thật (loại, ngày, lý do) — biết đơn đang chờ QL/HR và khi nào có hiệu lực.", 4,
                    "guides/guide-leave.md"),
            new Seed("guide-approval-inbox", "Hộp thư duyệt", "Duyệt",
                    "Ảnh tab Chờ tôi duyệt — Duyệt / Từ chối (không Designer).", 5,
                    "guides/guide-approval-inbox.md"),
            new Seed("guide-attendance-settings", "Chấm công GPS / WiFi", "Nhân sự",
                    "Ảnh ô GPS/WiFi + bản đồ — đặt vùng FTECH HN rồi lưu.", 6,
                    "guides/guide-attendance-settings.md"),
            new Seed("guide-hire", "Tuyển dụng & nhận việc", "Nhân sự",
                    "Ảnh form tin + Kanban/Onboarding — tin ENG-BE-01 đến checklist nhận việc.", 7,
                    "guides/guide-hire.md"),
            new Seed("guide-customers", "Khách hàng & avatar", "CRM",
                    "Ảnh list + form Avatar — KH001–KH010, xem nhanh / 360° đổi ảnh.", 8,
                    "guides/guide-customers.md"),
            new Seed("guide-deals", "Cơ hội bán", "CRM",
                    "Phễu bán hàng + núm kéo cột — thêm cơ hội, đã chốt / thất bại.", 9,
                    "guides/guide-deals.md"),
            new Seed("guide-workflows", "Phân quy trình duyệt", "Duyệt",
                    "Draft Designer → áp Cấu hình luồng — đơn Tuấn QL→HR; duyệt ở Hộp thư.", 10,
                    "guides/guide-workflows.md"),
            new Seed("guide-approval-attach", "Gắn luồng duyệt vào quy trình hệ thống", "Duyệt",
                    "Ba luồng FTECH + ảnh — nghỉ / mua / khóa kỳ lương vào Hộp thư.", 11,
                    "guides/guide-approval-attach.md"),
            new Seed("guide-qlts", "Quản lý tài sản", "Tài sản",
                    "LT-IT-015 → cấp phát Bảo — duyệt Hộp thư rồi bàn giao.", 12,
                    "guides/guide-qlts.md"),
            new Seed("guide-asset-assign", "Yêu cầu cấp phát tài sản", "Tài sản",
                    "Gửi từ tab Tài sản — duyệt Hộp thư / bàn giao (FTECH).", 13,
                    "guides/guide-asset-assign.md"),
            new Seed("guide-depreciation", "Khấu hao tài sản", "Tài sản",
                    "Sinh lịch 36 tháng — xem trước rồi ghi sổ (ảnh chứng từ).", 14,
                    "guides/guide-depreciation.md"),
            new Seed("guide-articles", "Bài viết nội bộ", "Nội bộ",
                    "Nháp Quốc khánh → Mai duyệt Hộp thư → xuất bản (mã ART-…).", 15,
                    "guides/guide-articles.md"),
            new Seed("guide-mobile", "Trên điện thoại — Bắt đầu", "Mobile",
                    "Ảnh đăng nhập + thanh thẻ — vào Trang chủ và Truy cập nhanh.", 16,
                    "guides/guide-mobile.md"),
            new Seed("guide-mobile-attendance", "Trên điện thoại — Chấm công", "Mobile",
                    "Check-in trong vùng — ngoài vùng thì giải trình (lý do mẫu).", 17,
                    "guides/guide-mobile-attendance.md"),
            new Seed("guide-mobile-leave", "Trên điện thoại — Nghỉ phép", "Mobile",
                    "Ảnh thẻ Nghỉ — xin phép 10–12/08, theo dõi badge, duyệt app.", 18,
                    "guides/guide-mobile-leave.md"),
            new Seed("guide-mobile-payslip", "Trên điện thoại — Phiếu lương", "Mobile",
                    "Ảnh phiếu kỳ 07/2026 — Gross/Net, PDF, xác nhận đã nhận.", 19,
                    "guides/guide-mobile-payslip.md"),
            new Seed("guide-accounting", "Kế toán — đi hết một kỳ", "Kế toán",
                    "Ảnh menu KT — bốn chặng một kỳ + ai làm việc nào.", 20,
                    "guides/guide-accounting.md"),
            new Seed("guide-accounting-setup", "Kế toán — thiết lập ban đầu", "Kế toán",
                    "FTECH: TT133 · Seed COA · năm 2026 · khóa kỳ 06/2026.", 21,
                    "guides/guide-accounting-setup.md"),
            new Seed("guide-accounting-journal", "Kế toán — chứng từ và ghi sổ", "Kế toán",
                    "Ảnh KPI kỳ — soát Nợ/Có, Ghi sổ, đảo khi sai TK lương.", 22,
                    "guides/guide-accounting-journal.md"),
            new Seed("guide-accounting-bank", "Kế toán — đối soát ngân hàng", "Kế toán",
                    "Ảnh wizard Import — khớp sao kê VCB rồi Khoá.", 23,
                    "guides/guide-accounting-bank.md"),
            new Seed("guide-accounting-reports", "Kế toán — sổ cái và báo cáo", "Kế toán",
                    "Ảnh sổ cái + cân đối — TK 334, Sổ CÂN, BCĐKT / KQKD.", 24,
                    "guides/guide-accounting-reports.md"),
            new Seed("guide-warehouse-reorder-rules", "Quy tắc tái nhập kho", "Kho",
                    "Min/Max theo SP+kho — cảnh báo tồn, SL đặt lại, tạo PR từ alert.", 25,
                    "guides/guide-warehouse-reorder-rules.md"),
            new Seed("guide-warehouse-grn-gin", "Phiếu nhập & xuất kho", "Kho",
                    "GRN/PNK tăng tồn · GIN/PXK giảm tồn — chỉ khi Xác nhận.", 26,
                    "guides/guide-warehouse-grn-gin.md"),
            new Seed("guide-warehouse-stock-takes", "Kiểm kê kho", "Kho",
                    "Đối chiếu tồn sổ vs đếm thực tế — 4 bước: tạo phiếu → đếm → gửi → điều chỉnh.", 27,
                    "guides/guide-warehouse-stock-takes.md"),
            new Seed("guide-warehouse-sme-rau-cu", "Kho rau củ — quy trình SME", "Kho",
                    "Nhập theo lô · xuất FEFO · hao hụt riêng · zone nhiệt độ · cảnh báo cận hạn.", 28,
                    "guides/guide-warehouse-sme-rau-cu.md"),
            new Seed("guide-hr-onboarding", "Onboarding & thử việc", "Nhân sự",
                    "Cấp thiết bị · đào tạo · mentor · đánh giá hết thử việc.", 29,
                    "guides/guide-hr-onboarding.md"),
            new Seed("guide-hr-payroll", "Chấm công & bảng lương", "Nhân sự",
                    "Chấm công → duyệt nghỉ → tổng hợp công → tính lương → chi trả.", 30,
                    "guides/guide-hr-payroll.md"),
            new Seed("guide-hr-kpi", "KPI & OKR", "Nhân sự",
                    "Đặt mục tiêu · check-in giữa kỳ · đánh giá cuối kỳ · cập nhật hồ sơ.", 31,
                    "guides/guide-hr-kpi.md"),
            new Seed("guide-hr-offboarding", "Nghỉ việc & offboarding", "Nhân sự",
                    "Đề xuất nghỉ → bàn giao TS → quyết toán lương → thu hồi tài khoản.", 32,
                    "guides/guide-hr-offboarding.md"),
            new Seed("guide-accounting-revenue", "Ghi nhận doanh thu", "Kế toán",
                    "Hoá đơn bán → thu công nợ → hạch toán kỳ → báo cáo KQKD.", 33,
                    "guides/guide-accounting-revenue.md"),
            new Seed("guide-accounting-tax", "Kê khai thuế GTGT", "Kế toán",
                    "Tổng hợp HĐ đầu vào/ra → tính GTGT → lập tờ khai → nộp & lưu CT.", 34,
                    "guides/guide-accounting-tax.md"),
            new Seed("guide-contract-digital", "Hợp đồng số & ký điện tử", "Hợp đồng",
                    "Soạn mẫu → duyệt nội bộ → ký OTP → xác thực → kho HĐ.", 35,
                    "guides/guide-contract-digital.md")
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
