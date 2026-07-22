package com.frezo.qlns.service.impl.payroll;

import com.frezo.qlns.entity.InsuranceConfig;
import com.frezo.qlns.entity.PayrollConfig;
import com.frezo.qlns.entity.TaxConfig;
import com.frezo.qlns.repository.InsuranceConfigRepository;
import com.frezo.qlns.repository.PayrollConfigRepository;
import com.frezo.qlns.repository.TaxConfigRepository;
import com.frezo.qtht.dto.response.PayrollSettingResponse;
import com.frezo.qtht.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Gom việc load các cấu hình cần thiết cho 1 kỳ lương của 1 tổ chức:
 * {@link PayrollConfig}, {@link InsuranceConfig}, biểu thuế {@link TaxConfig} và
 * cấu hình chấm công chuẩn ({@link PayrollSettingResponse}).
 * <p>
 * Đóng gói thành {@link ConfigBundle} để orchestrator chỉ inject 1 dep thay vì 4.
 */
@Component
@RequiredArgsConstructor
public class PayrollConfigLoader {

    private final PayrollConfigRepository payrollConfigRepository;
    private final InsuranceConfigRepository insuranceConfigRepository;
    private final TaxConfigRepository taxConfigRepository;
    private final SettingService settingService;

    /** Snapshot cấu hình lương cho 1 org + 1 năm — immutable trong scope 1 lần tính. */
    public record ConfigBundle(
            PayrollConfig payrollConfig,
            InsuranceConfig insuranceConfig,
            List<TaxConfig> taxConfigs,
            PayrollSettingResponse setting) {

        /** Số ngày công chuẩn theo cấu hình tổ chức, mặc định 26 nếu chưa cấu hình. */
        public int standardDays() {
            return setting != null && setting.getStandardWorkingDays() != null
                    ? setting.getStandardWorkingDays() : 26;
        }

        /** Số giờ làm việc chuẩn / ngày (dùng cho OT hourly rate). */
        public int standardHoursPerDay() {
            return payrollConfig != null && payrollConfig.getStandardHoursPerDay() != null
                    ? payrollConfig.getStandardHoursPerDay() : 8;
        }
    }

    /** Load toàn bộ config cần thiết cho tính lương của {@code personId} năm {@code year}. */
    public ConfigBundle load(String orgId, Integer year) {
        PayrollSettingResponse setting = settingService.getPayrollSetting(orgId);
        PayrollConfig payrollConfig = payrollConfigRepository
                .findByOrgIdAndYearAndIsActiveTrue(orgId, year).orElse(null);
        InsuranceConfig insuranceConfig = insuranceConfigRepository
                .findByYearAndIsActiveTrue(year).orElse(null);
        List<TaxConfig> taxConfigs = taxConfigRepository
                .findByYearAndIsActiveTrueOrderByBracketOrderAsc(year);
        return new ConfigBundle(payrollConfig, insuranceConfig, taxConfigs, setting);
    }

    /**
     * Cảnh báo thiếu Insurance/Tax config theo năm — dùng cho calculate-all summary
     * (tránh silent ZERO BHXH/TNCN khiến user tưởng tính đủ).
     */
    public List<String> missingYearConfigWarnings(Integer year) {
        List<String> warnings = new ArrayList<>();
        if (insuranceConfigRepository.findByYearAndIsActiveTrue(year).isEmpty()) {
            warnings.add("Thiếu config BHXH năm " + year + " — BHXH/BHYT/BHTN có thể bằng 0.");
        }
        if (taxConfigRepository.findByYearAndIsActiveTrueOrderByBracketOrderAsc(year).isEmpty()) {
            warnings.add("Thiếu config TNCN năm " + year + " — thuế TNCN có thể bằng 0.");
        }
        return warnings;
    }
}
