package com.frezo.qtbv.config;

import com.frezo.qtbv.entity.Asset;
import com.frezo.qtbv.entity.Category;
import com.frezo.qtbv.entity.CategoryGroup;
import com.frezo.qtbv.repository.AssetRepository;
import com.frezo.qtbv.repository.CategoryGroupRepository;
import com.frezo.qtbv.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Seed dữ liệu mẫu cho Quản lý tài sản (QLTS).
 * <p>
 * Seed 2 layer:
 * <ol>
 *   <li>{@code CategoryGroup(code='LoaiTaiSan')} + {@code Category} (Laptop, Desk, Chair, ...)
 *       — để dropdown loại tài sản có option chọn. Group code là <b>LoaiTaiSan</b> để nhất
 *       quán với convention FE ({@code GROUP_CODE_OPTIONS} trong {@code category.schema.ts})
 *       và để trang <code>/admin/category-management</code> hiển thị đúng nhóm.</li>
 *   <li>{@link Asset} — ~15 tài sản mẫu (đa dạng loại + status) để trang có content ngay khi
 *       demo.</li>
 * </ol>
 * <p>
 * <b>Idempotent</b>: skip nếu bảng {@code asset} đã có row (chống ghi đè khi restart).
 * <p>
 * <b>Migration</b>: nếu đã seed nhầm với {@code groupCode='QLTS'} ở version trước, seeder này
 * sẽ tự move sang {@code LoaiTaiSan} rồi xoá group QLTS mồ côi.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssetDataInitializer {

    private static final String GROUP_CODE = "LoaiTaiSan";
    private static final String GROUP_NAME = "Loại Tài Sản";
    /** Group code cũ đã seed ở phiên bản trước (deprecated) — cần migrate sang LoaiTaiSan. */
    private static final String LEGACY_GROUP_CODE = "QLTS";

    private final AssetRepository assetRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryGroupRepository categoryGroupRepository;
    private final JdbcTemplate jdbcTemplate;

    private record CatSeed(String code, String name, int order) {}
    private record AssetSeed(
            String code, String name, String cat, String brand, String model, String sn,
            LocalDate purchaseDate, long price, LocalDate warranty, String status, String location, String note
    ) {}

    private static final List<CatSeed> CATEGORIES = List.of(
            new CatSeed("LAPTOP",   "Laptop",     1),
            new CatSeed("PC",       "Máy bàn",    2),
            new CatSeed("MONITOR",  "Màn hình",   3),
            new CatSeed("PHONE",    "Điện thoại", 4),
            new CatSeed("PRINTER",  "Máy in",     5),
            new CatSeed("DESK",     "Bàn làm việc", 6),
            new CatSeed("CHAIR",    "Ghế văn phòng", 7),
            new CatSeed("CABINET",  "Tủ hồ sơ",   8),
            new CatSeed("VEHICLE",  "Phương tiện", 9),
            new CatSeed("OTHER",    "Khác",       99)
    );

    private static final List<AssetSeed> ASSETS = List.of(
            new AssetSeed("AS-2026-0001", "MacBook Pro 14 M3",      "LAPTOP",  "Apple",     "MBP14 M3 Pro", "C02XYZ001",
                    LocalDate.of(2025, 3, 15),  55_000_000L, LocalDate.of(2026, 3, 15), "IN_USE",     "Tầng 3, phòng Dev", "Máy chính cho lead BE"),
            new AssetSeed("AS-2026-0002", "Dell XPS 13",            "LAPTOP",  "Dell",      "XPS 13 9315", "DL13-002",
                    LocalDate.of(2025, 5, 10),  32_000_000L, LocalDate.of(2027, 5, 10), "IN_USE",     "Tầng 3, phòng Dev", null),
            new AssetSeed("AS-2026-0003", "ThinkPad X1 Carbon G11", "LAPTOP",  "Lenovo",    "X1 Carbon 11", "TP-X1-003",
                    LocalDate.of(2025, 8, 20),  38_000_000L, LocalDate.of(2028, 8, 20), "AVAILABLE",  "Kho IT tầng 1", "Vừa thu hồi từ nhân viên nghỉ việc"),
            new AssetSeed("AS-2026-0004", "MacBook Air M2",         "LAPTOP",  "Apple",     "MBA M2",       "C02XYZ004",
                    LocalDate.of(2024, 11, 5),  28_000_000L, LocalDate.of(2025, 11, 5), "IN_USE",     "Tầng 4, phòng Design", "Hết BH cuối 2025"),
            new AssetSeed("AS-2026-0005", "iMac 24 M3",             "PC",      "Apple",     "iMac 24 M3",  "IM-005",
                    LocalDate.of(2024, 6, 1),   45_000_000L, LocalDate.of(2025, 6, 1),  "MAINTENANCE", "TT bảo hành Apple", "Đem đi thay bàn phím"),
            new AssetSeed("AS-2026-0006", "PC Workstation Intel i9","PC",      "HP",        "Z2 G9",       "HP-Z2-006",
                    LocalDate.of(2025, 2, 28),  35_000_000L, LocalDate.of(2028, 2, 28), "IN_USE",     "Phòng thiết kế 3D",   null),
            new AssetSeed("AS-2026-0007", "Dell Ultrasharp 27 4K",  "MONITOR", "Dell",      "U2723QE",     "DL-U27-007",
                    LocalDate.of(2025, 4, 10),  12_500_000L, LocalDate.of(2028, 4, 10), "IN_USE",     "Tầng 3, phòng Dev", null),
            new AssetSeed("AS-2026-0008", "LG Ultrafine 32",        "MONITOR", "LG",        "32UN880",     "LG-32-008",
                    LocalDate.of(2025, 4, 10),  13_800_000L, LocalDate.of(2028, 4, 10), "AVAILABLE",  "Kho IT tầng 1", null),
            new AssetSeed("AS-2026-0009", "iPhone 15 Pro (Test)",   "PHONE",   "Apple",     "iPhone 15 Pro", "IMEI-009",
                    LocalDate.of(2025, 10, 1),  28_000_000L, LocalDate.of(2026, 10, 1), "AVAILABLE",  "Kho QA",              "Dùng để test app mobile"),
            new AssetSeed("AS-2026-0010", "Samsung Galaxy S24",     "PHONE",   "Samsung",   "S24 Ultra",   "IMEI-010",
                    LocalDate.of(2025, 6, 15),  25_000_000L, LocalDate.of(2026, 6, 15), "AVAILABLE",  "Kho QA",              "Dùng test Android"),
            new AssetSeed("AS-2026-0011", "HP LaserJet Pro M404",   "PRINTER", "HP",        "M404dn",      "HP-LJ-011",
                    LocalDate.of(2024, 3, 8),    6_500_000L, LocalDate.of(2026, 3, 8),  "IN_USE",     "Tầng 2, HC-NS",       null),
            new AssetSeed("AS-2026-0012", "Bàn làm việc gỗ 1m4",    "DESK",    "IKEA",      "BEKANT",      null,
                    LocalDate.of(2023, 9, 1),    3_200_000L, null,                        "IN_USE",     "Tầng 3, phòng Dev", "Set đồng bộ với ghế"),
            new AssetSeed("AS-2026-0013", "Ghế công thái học ErgoChair 2", "CHAIR", "Autonomous", "ErgoChair Pro", null,
                    LocalDate.of(2023, 9, 1),    8_500_000L, LocalDate.of(2025, 9, 1),  "IN_USE",     "Tầng 3, phòng Dev", null),
            new AssetSeed("AS-2026-0014", "Tủ hồ sơ 5 ngăn",        "CABINET", "Hoà Phát",  "TU986",       null,
                    LocalDate.of(2022, 5, 20),   2_800_000L, null,                        "AVAILABLE",  "Kho HC-NS",           null),
            new AssetSeed("AS-2026-0015", "Xe máy Honda Wave RSX",  "VEHICLE", "Honda",     "Wave RSX FI", "29-P1 12345",
                    LocalDate.of(2024, 1, 10),  22_000_000L, LocalDate.of(2027, 1, 10), "AVAILABLE",  "Bãi xe tầng hầm",     "Xe công ty giao hàng")
    );

    @PostConstruct
    public void init() {
        migrateLegacyGroup();
        seedGroup();
        seedCategories();
        seedAssets();
    }

    /**
     * Migration one-shot: nếu tồn tại category nào có {@code group_code='QLTS'} (di sản từ
     * phiên bản seeder cũ) → chuyển sang {@code LoaiTaiSan}, sau đó xoá group QLTS mồ côi.
     * Dùng {@link JdbcTemplate} thay vì JPA để cập nhật hàng loạt nhanh và không đụng cache.
     */
    private void migrateLegacyGroup() {
        try {
            // Đảm bảo group mới tồn tại TRƯỚC khi update FK
            seedGroup();
            int moved = jdbcTemplate.update(
                    "UPDATE categories SET group_code = ? WHERE group_code = ?",
                    GROUP_CODE, LEGACY_GROUP_CODE);
            if (moved > 0) {
                log.info("[migrate] Đã chuyển {} category từ QLTS → LoaiTaiSan.", moved);
            }
            // Xoá group cũ nếu không còn ai reference
            Long remain = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM categories WHERE group_code = ?",
                    Long.class, LEGACY_GROUP_CODE);
            if (remain == null || remain == 0) {
                int dropped = jdbcTemplate.update(
                        "DELETE FROM category_group WHERE code = ?", LEGACY_GROUP_CODE);
                if (dropped > 0) log.info("[migrate] Đã xoá group QLTS mồ côi.");
            }
        } catch (Exception ex) {
            log.warn("[migrate] Bỏ qua migration QLTS → LoaiTaiSan: {}", ex.getMessage());
        }
    }

    /** Ensure {@code category_group(code='LoaiTaiSan')} tồn tại để FK không văng. */
    private void seedGroup() {
        if (categoryGroupRepository.findById(GROUP_CODE).isPresent()) return;
        CategoryGroup g = new CategoryGroup();
        g.setCode(GROUP_CODE);
        g.setName(GROUP_NAME);
        g.setCatGroup(1);
        g.setIsDeleted(false);
        categoryGroupRepository.save(g);
        log.info("[seed] Đã tạo CategoryGroup {}.", GROUP_CODE);
    }

    private void seedCategories() {
        int inserted = 0;
        for (CatSeed c : CATEGORIES) {
            if (categoryRepository.findByCode(c.code()).isPresent()) continue;
            categoryRepository.save(Category.builder()
                    .code(c.code())
                    .name(c.name())
                    .groupCode(GROUP_CODE)
                    .orderIndex(c.order())
                    .active(true)
                    .build());
            inserted++;
        }
        if (inserted > 0) log.info("[seed] Đã thêm {} loại tài sản.", inserted);
    }

    private void seedAssets() {
        // Idempotent — chỉ seed khi bảng asset còn trống (không tính soft-deleted)
        long existing = assetRepository.count();
        if (existing > 0) {
            log.info("[seed] Asset đã có {} record → skip.", existing);
            return;
        }
        log.info("[seed] Bắt đầu seed {} tài sản mẫu...", ASSETS.size());
        int inserted = 0;
        for (AssetSeed a : ASSETS) {
            if (assetRepository.findByCode(a.code()).isPresent()) continue;
            assetRepository.save(Asset.builder()
                    .code(a.code())
                    .name(a.name())
                    .categoryCode(a.cat())
                    .brand(a.brand())
                    .model(a.model())
                    .serialNumber(a.sn())
                    .purchaseDate(a.purchaseDate())
                    .purchasePrice(BigDecimal.valueOf(a.price()))
                    .currentValue(BigDecimal.valueOf(a.price())) // simplified: currentValue = purchase (chưa depreciate)
                    .warrantyEndDate(a.warranty())
                    .status(a.status())
                    .location(a.location())
                    .note(a.note())
                    .build());
            inserted++;
        }
        log.info("[seed] Đã thêm {} tài sản mẫu.", inserted);
    }
}
