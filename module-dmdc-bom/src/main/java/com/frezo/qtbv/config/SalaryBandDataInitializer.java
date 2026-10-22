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


@Slf4j
@Component
@RequiredArgsConstructor
public class SalaryBandDataInitializer {

    private static final String GROUP_CODE = "SalaryBand";
    private static final String GROUP_NAME = "Bậc lương";
    private static final int GROUP_CAT = 1;

    private final CategoryRepository categoryRepository;
    private final CategoryGroupRepository categoryGroupRepository;

    private record BandSeed(
            String code, String name, String shortName, String fn,
            long min, long max, long target, int orderIndex, String note
    ) {}


    private static final List<BandSeed> SEEDS = List.of(

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


            new BandSeed("ENG-SR",    "Senior Engineer",     "ENG-SR",   "ENG",  45_000_000L,  75_000_000L,  58_000_000L, 20,
                    "Backend/Fullstack/Mobile 5+ năm, đọc source lib chính"),
            new BandSeed("ENG-STAFF", "Staff Engineer",      "STAFF",    "ENG",  70_000_000L, 120_000_000L,  92_000_000L, 21,
                    "IC path cao nhất, tech lead cross-team, kiến trúc hệ thống")
    );

    @PostConstruct
    public void init() {
        ensureGroupExists();


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
