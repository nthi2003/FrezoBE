package com.frezo.qtbv.config;

import com.frezo.qtbv.depreciation.DepreciationCalculator;
import com.frezo.qtbv.depreciation.DepreciationConstants;
import com.frezo.qtbv.entity.Asset;
import com.frezo.qtbv.entity.DepreciationSchedule;
import com.frezo.qtbv.repository.AssetRepository;
import com.frezo.qtbv.repository.DepreciationScheduleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Seed lịch khấu hao demo (idempotent) — để trang {@code /assets/depreciation} có dòng ngay sau restart.
 * <p>Chỉ tạo schedule cho một số tài sản mẫu có giá mua; bỏ qua nếu đã có schedule (unique asset_id).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DepreciationDataInitializer {

    /** Mã TS demo ưu tiên (khớp AssetDataInitializer). */
    private static final List<String> DEMO_ASSET_CODES = List.of(
            "AS-2026-0001",
            "AS-2026-0002",
            "AS-2026-0005",
            "AS-2026-0007",
            "AS-2026-0015"
    );

    private static final int DEFAULT_MONTHS = 36;

    private final AssetRepository assetRepository;
    private final DepreciationScheduleRepository scheduleRepository;

    @PostConstruct
    public void init() {
        seedDemoSchedules();
    }

    private void seedDemoSchedules() {
        int inserted = 0;
        for (String code : DEMO_ASSET_CODES) {
            Asset asset = assetRepository.findByCode(code).orElse(null);
            if (asset == null || Boolean.TRUE.equals(asset.getIsDeleted())) {
                continue;
            }
            if (asset.getPurchasePrice() == null || asset.getPurchasePrice().signum() <= 0) {
                continue;
            }
            if (scheduleRepository.findByAssetIdAndIsDeletedFalse(asset.getId()).isPresent()) {
                continue;
            }
            LocalDate start = asset.getPurchaseDate() != null
                    ? asset.getPurchaseDate()
                    : LocalDate.of(2025, 1, 1);
            BigDecimal monthly = DepreciationCalculator.monthlyStraightLine(
                    asset.getPurchasePrice(), DEFAULT_MONTHS);
            scheduleRepository.save(DepreciationSchedule.builder()
                    .assetId(asset.getId())
                    .method(DepreciationConstants.METHOD_STRAIGHT_LINE)
                    .startDate(start)
                    .months(DEFAULT_MONTHS)
                    .monthlyAmount(monthly)
                    .remainingValue(asset.getPurchasePrice())
                    .status(DepreciationConstants.SCHEDULE_ACTIVE)
                    .build());
            inserted++;
        }
        if (inserted > 0) {
            log.info("[seed] Đã thêm {} lịch khấu hao demo.", inserted);
        } else {
            log.info("[seed] Lịch khấu hao demo đã có hoặc chưa có tài sản phù hợp → skip.");
        }
    }
}
