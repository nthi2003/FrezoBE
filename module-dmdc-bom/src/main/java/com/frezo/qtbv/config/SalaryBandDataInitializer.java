package com.frezo.qtbv.config;

import com.frezo.qtbv.entity.Category;
import com.frezo.qtbv.entity.CategoryGroup;
import com.frezo.qtbv.repository.CategoryGroupRepository;
import com.frezo.qtbv.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seed data cho Bậc lương (SalaryBand) — hiển thị ở /qlns/salary-bands.
 * <p>
 * <b>Vì sao dùng Category?</b> — FE {@code SalaryBandsPage} persist qua endpoint
 * {@code /qtbv/category} với {@code groupCode='SalaryBand'} và pack meta
 * ({@code min/max/target/function/currency}) vào field {@code description} dạng JSON compact.
 * <p>
 * <b>FK constraint</b>: {@code categories.group_code → category_group.code} — nên phải
 * seed row {@code CategoryGroup(code='SalaryBand')} TRƯỚC khi insert categories, nếu không
 * sẽ dính {@code fk47ciexmcyhg1g1wljbg472f13}.
 * <p>
 * <b>Không dùng {@code @Transactional} ở {@code @PostConstruct}</b>: proxy chưa init xong
 * khi hook chạy → transaction advice bị bypass. Thay vì thế, dựa vào tính chất
 * {@code JpaRepository#save} tự có transaction (defined trong {@code SimpleJpaRepository})
 * để mỗi INSERT tự commit — đủ cho use case seeder này.
 * <p>
 * <b>Số liệu tham khảo</b>: Radford Vietnam Q2/2026, Robert Half, Anphabe report.
 * <p>
 * <b>Idempotent</b>: chỉ seed khi chưa có band nào — không ghi đè nếu admin đã tạo tay.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalaryBandDataInitializer {

    private static final String GROUP_CODE = "SalaryBand";
    private static final String GROUP_NAME = "Bậc lương";
    /** {@code cat_group} là int flag phân loại — dùng 1 = business (khớp các group hiện có). */
    private static final int GROUP_CAT = 1;

    private final CategoryRepository categoryRepository;
    private final CategoryGroupRepository categoryGroupRepository;

    /**
     * Định nghĩa gọn 1 dòng cho readability. currency mặc định VND, tất cả active.
     * (code, name, shortName, function, min, max, target, orderIndex, note)
     */
    private record BandSeed(
            String code, String name, String shortName, String fn,
            long min, long max, long target, int orderIndex, String note
    ) {}

    /**
     * Ladder 12 bậc cho SMB — từ Thực tập → C-Suite.
     * Range Min↔Max ~30-80% (theo Radford best practice, hơi rộng cho VN market).
     * ENG tách 2 bậc riêng vì market rate cao hơn +25-40% so với chung.
     */
    private static final List<BandSeed> SEEDS = List.of(
            // ---- Chung (function=ALL) ----
            new BandSeed("P0-INTERN", "Thực tập sinh",       "Intern",  "ALL",   5_000_000L,   8_000_000L,   6_500_000L, 1,
                    "Sinh viên năm cuối, học việc 3-6 tháng"),
            new BandSeed("P1-JR",     "Junior",              "JR",      "ALL",  10_000_000L,  18_000_000L,  14_000_000L, 2,
                    "0-2 năm KN, làm task nhỏ có mentor"),
            new BandSeed("P2-MID",    "Middle",              "MID",     "ALL",  18_000_000L,  32_000_000L,  25_000_000L, 3,
                    "2-4 năm KN, own được feature vừa"),
            new BandSeed("P3-SR",     "Senior",              "SR",      "ALL",  32_000_000L,  55_000_000L,  42_000_000L, 4,
                    "4-7 năm KN, thiết kế module, mentor JR"),
            new BandSeed("P4-LEAD",   "Team Lead",           "LEAD",    "ALL",  45_000_000L,  75_000_000L,  58_000_000L, 5,
                    "Dẫn team 3-8 người, review code, chịu trách nhiệm delivery"),
            new BandSeed("P5-MGR",    "Manager",             "MGR",     "ALL",  60_000_000L, 100_000_000L,  78_000_000L, 6,
                    "Quản lý phòng ban 10-20 người, budget, hire"),
            new BandSeed("P6-SR-MGR", "Senior Manager",      "SR-MGR",  "ALL",  85_000_000L, 140_000_000L, 110_000_000L, 7,
                    "Quản lý multi-team, strategy quý"),
            new BandSeed("P7-DIR",    "Director",            "DIR",     "ALL", 130_000_000L, 220_000_000L, 170_000_000L, 8,
                    "Trách nhiệm P&L 1 mảng, báo cáo BOD"),
            new BandSeed("P8-VP",     "Vice President",      "VP",      "ALL", 200_000_000L, 320_000_000L, 260_000_000L, 9,
                    "Phó chủ tịch mảng, chiến lược năm"),
            new BandSeed("P9-C",      "C-Level",             "C-SUITE", "ALL", 300_000_000L, 600_000_000L, 450_000_000L, 10,
                    "CEO/CTO/CFO — thoả thuận riêng + ESOP"),

            // ---- Kỹ thuật (function=ENG) — market rate cao hơn ----
            new BandSeed("ENG-SR",    "Senior Engineer",     "ENG-SR",   "ENG",  45_000_000L,  75_000_000L,  58_000_000L, 20,
                    "Backend/Fullstack/Mobile 5+ năm, đọc source lib chính"),
            new BandSeed("ENG-STAFF", "Staff Engineer",      "STAFF",    "ENG",  70_000_000L, 120_000_000L,  92_000_000L, 21,
                    "IC path cao nhất, tech lead cross-team, kiến trúc hệ thống")
    );

    @PostConstruct
    public void init() {
        ensureGroupExists();

        // Idempotent: chỉ seed khi group SalaryBand hoàn toàn trống
        long existing = categoryRepository.findAll().stream()
                .filter(c -> GROUP_CODE.equals(c.getGroupCode()))
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .count();
        if (existing > 0) {
            log.info("[seed] SalaryBand đã có {} record → skip seed.", existing);
            return;
        }

        log.info("[seed] Bắt đầu seed {} bậc lương mẫu (Radford VN 2026)...", SEEDS.size());

        int inserted = 0;
        for (BandSeed s : SEEDS) {
            if (categoryRepository.findByCode(s.code()).isPresent()) continue;
            categoryRepository.save(Category.builder()
                    .code(s.code())
                    .name(s.name())
                    .shortName(s.shortName())
                    .groupCode(GROUP_CODE)
                    .orderIndex(s.orderIndex())
                    .description(buildMetaJson(s))
                    .active(true)
                    .build());
            inserted++;
        }

        log.info("[seed] SalaryBand: inserted={} bậc lương.", inserted);
    }

    /**
     * Đảm bảo có row {@code category_group(code='SalaryBand')} trước khi INSERT categories,
     * tránh FK violation. {@code CategoryGroup.id} = {@code code} (không phải UUID),
     * nên {@code findById(GROUP_CODE)} là PK lookup.
     * {@code save()} tự có transaction (Spring Data proxy) → không cần {@code @Transactional} ngoài.
     */
    private void ensureGroupExists() {
        if (categoryGroupRepository.findById(GROUP_CODE).isPresent()) {
            log.debug("[seed] CategoryGroup '{}' đã tồn tại → skip.", GROUP_CODE);
            return;
        }
        CategoryGroup g = new CategoryGroup();
        g.setCode(GROUP_CODE);
        g.setName(GROUP_NAME);
        g.setCatGroup(GROUP_CAT);
        g.setIsDeleted(false);
        categoryGroupRepository.save(g);
        log.info("[seed] Đã tạo CategoryGroup '{}' = '{}'.", GROUP_CODE, GROUP_NAME);
    }

    /**
     * Pack meta JSON đúng schema mà FE {@code parseMeta} kỳ vọng:
     * {@code {min,max,target,currency,function,note}}.
     * Cố tình build tay bằng String để tránh add Jackson dep chỉ cho seeder.
     * Đảm bảo escape các dấu " trong note.
     */
    private String buildMetaJson(BandSeed s) {
        String note = s.note() == null ? "" : s.note().replace("\"", "'");
        return "{"
                + "\"min\":" + s.min() + ","
                + "\"max\":" + s.max() + ","
                + "\"target\":" + s.target() + ","
                + "\"currency\":\"VND\","
                + "\"function\":\"" + s.fn() + "\","
                + "\"note\":\"" + note + "\""
                + "}";
    }
}
