package com.frezo.profit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.profit.common.CostCategoryCode;
import com.frezo.profit.common.CostType;
import com.frezo.profit.common.Salary3P;
import com.frezo.profit.dto.response.ProfitDashboardResponse;
import com.frezo.profit.dto.response.ProfitSalesKpiResponse;
import com.frezo.profit.dto.response.ProfitSettingDto;
import com.frezo.profit.entity.ProfitAnalysisSnapshot;
import com.frezo.profit.entity.ProfitCostCategory;
import com.frezo.profit.entity.ProfitEmployeeAllocation;
import com.frezo.profit.entity.ProfitFixedOverhead;
import com.frezo.profit.entity.ProfitSetting;
import com.frezo.profit.repository.ProfitAnalysisSnapshotRepository;
import com.frezo.profit.repository.ProfitCostCategoryRepository;
import com.frezo.profit.repository.ProfitEmployeeAllocationRepository;
import com.frezo.profit.repository.ProfitFixedOverheadRepository;
import com.frezo.profit.repository.ProfitSettingRepository;
import com.frezo.profit.service.ProfitDashboardService;
import com.frezo.profit.service.ProfitSettingService;
import com.frezo.profit.service.support.ProfitContractSalaryResolver;
import com.frezo.product.repository.SaleOrderRepository;
import com.frezo.qlns.entity.Payroll;
import com.frezo.qlns.repository.PayrollRepository;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfitDashboardServiceImpl implements ProfitDashboardService {

    private static final String MARKET_VN = "VN";

    private final SaleOrderRepository saleOrderRepository;
    private final ProfitSettingRepository profitSettingRepository;
    private final ProfitSettingService profitSettingService;
    private final ProfitCostCategoryRepository costCategoryRepository;
    private final ProfitFixedOverheadRepository fixedOverheadRepository;
    private final ProfitEmployeeAllocationRepository employeeAllocationRepository;
    private final PayrollRepository payrollRepository;
    private final PersonRepository personRepository;
    private final ProfitAnalysisSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;
    private final ProfitContractSalaryResolver contractSalaryResolver;

    /** Tính toán dashboard lợi nhuận: doanh thu, chi phí, lãi ròng, break-even, mục tiêu theo kỳ. */
    @Override
    @Transactional(readOnly = true)
    public ProfitDashboardResponse getDashboard(int year, int month, boolean persistSnapshot) {
        ProfitSetting global = resolveGlobalSetting();
        ProfitSettingDto settingsDto = global.getId() != null
                ? profitSettingService.getById(global.getId())
                : ProfitSettingDto.builder()
                        .scope("GLOBAL")
                        .defaultCogsPct(global.getDefaultCogsPct())
                        .riskCostPct(global.getRiskCostPct())
                        .salesCommissionPct(global.getSalesCommissionPct())
                        .profitMarginPct(global.getProfitMarginPct())
                        .currency(global.getCurrency())
                        .build();

        double cogsPct = safePct(global.getDefaultCogsPct(), 55d);
        double riskPct = safePct(global.getRiskCostPct(), 2d);
        double commissionPct = safePct(global.getSalesCommissionPct(), 3d);
        double targetMarginPct = safePct(global.getProfitMarginPct(), 20d);
        double corporateTaxPct = safePct(global.getCorporateTaxPct(), 20d);
        String currency = global.getCurrency() != null ? global.getCurrency() : "VND";

        LocalDateTime from = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime to = LocalDate.of(year, month, 1).with(TemporalAdjusters.lastDayOfMonth())
                .plusDays(1).atStartOfDay();

        double actualRevenue = safe(saleOrderRepository.sumTotalAmountByCreatedDateBetween(from, to));

        List<ProfitEmployeeAllocation> allocations = employeeAllocationRepository.findByIsDeletedFalseAndActiveTrue();
        List<ProfitFixedOverhead> overheads = fixedOverheadRepository.findByYearAndMonthAndIsDeletedFalse(year, month);

        Map<String, ProfitCostCategory> categoryById = new HashMap<>();
        costCategoryRepository.findByMarketCodeAndIsDeletedFalseOrderBySortOrderAsc(MARKET_VN)
                .forEach(c -> categoryById.put(c.getId(), c));

        List<ProfitDashboardResponse.CostBreakdownLine> breakdown = new ArrayList<>();
        double fixedOverheadTotal = 0d;
        for (ProfitFixedOverhead oh : overheads) {
            fixedOverheadTotal += safe(oh.getAmount());
            ProfitCostCategory cat = categoryById.get(oh.getCategoryId());
            breakdown.add(ProfitDashboardResponse.CostBreakdownLine.builder()
                    .code(cat != null ? cat.getCode().name() : "FIXED")
                    .label(cat != null ? cat.getName() : "Chi phí cố định")
                    .costType(CostType.FIXED.name())
                    .amount(safe(oh.getAmount()))
                    .build());
        }

        double fixedRoleSalaries = 0d;
        double salesSalaries = 0d;
        int salesStaffCount = 0;
        for (ProfitEmployeeAllocation alloc : allocations) {
            double salary = resolveSalary(alloc, year, month);
            if (alloc.getCostCategoryCode() == CostCategoryCode.SALES_VARIABLE) {
                salesSalaries += salary;
                salesStaffCount++;
                breakdown.add(ProfitDashboardResponse.CostBreakdownLine.builder()
                        .code(CostCategoryCode.SALES_VARIABLE.name())
                        .label("Lương NVKD: " + resolvePersonName(alloc.getPersonId()))
                        .costType(CostType.VARIABLE.name())
                        .amount(salary)
                        .build());
            } else {
                fixedRoleSalaries += salary;
                breakdown.add(ProfitDashboardResponse.CostBreakdownLine.builder()
                        .code(alloc.getCostCategoryCode().name())
                        .label("Lương " + alloc.getCostCategoryCode().name() + ": " + resolvePersonName(alloc.getPersonId()))
                        .costType(CostType.FIXED.name())
                        .amount(salary)
                        .build());
            }
        }

        double totalFixedCosts = fixedOverheadTotal + fixedRoleSalaries;
        double monthlyBurn = totalFixedCosts + salesSalaries;

        double estimatedCogs = actualRevenue * cogsPct / 100d;
        double riskCost = actualRevenue * riskPct / 100d;
        double commissionCost = actualRevenue * commissionPct / 100d;
        double totalVariableCosts = riskCost + commissionCost;

        double contributionMarginPct = 100d - cogsPct - riskPct - commissionPct;
        double breakEvenRevenue = contributionMarginPct > 0
                ? monthlyBurn / (contributionMarginPct / 100d)
                : monthlyBurn;
        double targetRevenue = contributionMarginPct > 0
                ? (monthlyBurn * (1 + targetMarginPct / 100d)) / (contributionMarginPct / 100d)
                : monthlyBurn;

        double revenueShortfall = Math.max(0d, breakEvenRevenue - actualRevenue);
        double targetShortfall = Math.max(0d, targetRevenue - actualRevenue);
        double netProfit = actualRevenue - estimatedCogs - totalVariableCosts - totalFixedCosts - salesSalaries;
        double corporateTaxAmount = Salary3P.corporateTaxAmount(netProfit, corporateTaxPct);
        double netProfitAfterTax = Salary3P.netProfitAfterTax(netProfit, corporateTaxPct);

        ProfitDashboardResponse response = ProfitDashboardResponse.builder()
                .year(year)
                .month(month)
                .currency(currency)
                .actualRevenue(actualRevenue)
                .breakEvenRevenue(breakEvenRevenue)
                .targetRevenue(targetRevenue)
                .revenueShortfall(revenueShortfall)
                .targetShortfall(targetShortfall)
                .totalFixedCosts(totalFixedCosts)
                .totalVariableCosts(totalVariableCosts)
                .estimatedCogs(estimatedCogs)
                .netProfit(netProfit)
                .corporateTaxAmount(corporateTaxAmount)
                .netProfitAfterTax(netProfitAfterTax)
                .corporateTaxPct(corporateTaxPct)
                .monthlyBurn(monthlyBurn)
                .contributionMarginPct(contributionMarginPct)
                .employeeCount(allocations.size())
                .salesStaffCount(salesStaffCount)
                .costBreakdown(breakdown)
                .build();

        if (persistSnapshot) {
            persistSnapshot(year, month, currency, settingsDto, response);
        }

        return response;
    }

    /** Tính KPI bán hàng: mục tiêu, thực tế, % đạt được, hoa hồng, lương 3P cho từng nhân viên kinh doanh. */
    @Override
    @Transactional(readOnly = true)
    public ProfitSalesKpiResponse getSalesKpi(int year, int month) {
        ProfitSetting global = resolveGlobalSetting();
        String currency = global.getCurrency() != null ? global.getCurrency() : "VND";

        LocalDateTime from = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime to = LocalDate.of(year, month, 1).with(TemporalAdjusters.lastDayOfMonth())
                .plusDays(1).atStartOfDay();

        List<ProfitEmployeeAllocation> salesStaff = employeeAllocationRepository
                .findByCostCategoryCodeAndIsDeletedFalseAndActiveTrue(CostCategoryCode.SALES_VARIABLE);

        List<ProfitSalesKpiResponse.SalesKpiLine> lines = new ArrayList<>();
        for (ProfitEmployeeAllocation alloc : salesStaff) {
            Salary3P salary3p = contractSalaryResolver.resolve3P(alloc.getPersonId(), alloc.getContractId());
            double commPct = alloc.getCommissionPct() != null
                    ? alloc.getCommissionPct()
                    : safePct(global.getSalesCommissionPct(), 3d);
            double target;
            boolean autoTarget = Boolean.TRUE.equals(alloc.getAutoCalculateTarget());
            if (autoTarget) {
                target = contractSalaryResolver.resolveRevenueTarget(salary3p, global, alloc.getCommissionPct());
            } else {
                target = safe(alloc.getRevenueTargetMonthly());
            }
            Double rawActual = saleOrderRepository.sumTotalAmountByStaffIdAndCreatedDateBetween(
                    alloc.getPersonId(), from, to);
            double actual = rawActual != null ? rawActual : 0d;
            double shortfall = Math.max(0d, target - actual);
            double achievementPct = target > 0 ? (actual / target) * 100d : (actual > 0 ? 100d : 0d);
            double monthlySalary = resolveSalary(alloc, year, month);
            double estimatedCommission = actual * commPct / 100d;
            double p3Earned = Salary3P.p3Earned(salary3p, achievementPct);
            double totalComp = Salary3P.estimatedTotalComp(salary3p, achievementPct, actual, commPct);

            lines.add(ProfitSalesKpiResponse.SalesKpiLine.builder()
                    .personId(alloc.getPersonId())
                    .personName(resolvePersonName(alloc.getPersonId()))
                    .contractId(alloc.getContractId())
                    .revenueTarget(target)
                    .actualRevenue(actual)
                    .shortfall(shortfall)
                    .achievementPct(achievementPct)
                    .monthlySalary(monthlySalary)
                    .commissionPct(commPct)
                    .estimatedCommission(estimatedCommission)
                    .salaryP1(salary3p.p1Position())
                    .salaryP2(salary3p.p2Person())
                    .salaryP3(salary3p.p3Performance())
                    .salaryP3Earned(p3Earned)
                    .estimatedTotalComp(totalComp)
                    .targetAutoCalculated(autoTarget)
                    .build());
        }

        return ProfitSalesKpiResponse.builder()
                .year(year)
                .month(month)
                .currency(currency)
                .employees(lines)
                .build();
    }

    /** Lưu snapshot kết quả phân tích lợi nhuận vào database để audit trail. */
    private void persistSnapshot(int year, int month, String currency,
                                 ProfitSettingDto input, ProfitDashboardResponse result) {
        try {
            ProfitAnalysisSnapshot snap = ProfitAnalysisSnapshot.builder()
                    .year(year)
                    .month(month)
                    .currency(currency)
                    .calculatedAt(LocalDateTime.now())
                    .calculatedBy(SystemUtils.getCurrentUsername())
                    .inputJson(objectMapper.writeValueAsString(input))
                    .resultJson(objectMapper.writeValueAsString(result))
                    .build();
            snapshotRepository.save(snap);
        } catch (JsonProcessingException ignored) {
            // snapshot is best-effort audit trail
        }
    }

    /** Lấy cài đặt lợi nhuận toàn cục (GLOBAL), dùng giá trị mặc định nếu chưa có. */
    private ProfitSetting resolveGlobalSetting() {
        return profitSettingRepository.findByScopeAndScopeRefIdAndIsDeletedFalse("GLOBAL", null)
                .orElseGet(this::defaultGlobal);
    }

    /** Tạo đối tượng ProfitSetting mặc định khi chưa có cấu hình trong database. */
    private ProfitSetting defaultGlobal() {
        return ProfitSetting.builder()
                .scope("GLOBAL")
                .defaultCogsPct(55d)
                .riskCostPct(2d)
                .salesCommissionPct(3d)
                .profitMarginPct(20d)
                .corporateTaxPct(20d)
                .currency("VND")
                .active(true)
                .build();
    }

    /** Xác định lương thực tế: lấy từ bảng payroll nếu dùng payrollSource, nếu không lấy manually. */
    private double resolveSalary(ProfitEmployeeAllocation alloc, int year, int month) {
        if (Boolean.TRUE.equals(alloc.getUsePayrollSource())) {
            Optional<Payroll> payroll = payrollRepository
                    .findByPersonIdAndMonthAndYearAndIsDeletedFalseOrderByUpdatedDateDescIdDesc(
                            alloc.getPersonId(), month, year)
                    .stream()
                    .findFirst();
            if (payroll.isPresent() && payroll.get().getGrossSalary() != null) {
                return payroll.get().getGrossSalary().doubleValue();
            }
        }
        return safe(alloc.getMonthlySalary());
    }

    /** Tra cứu tên nhân viên theo personId, trả về personId nếu không tìm thấy. */
    private String resolvePersonName(String personId) {
        return personRepository.findById(personId)
                .filter(p -> p.getIsDeleted() == null || !p.getIsDeleted())
                .map(Person::getName)
                .orElse(personId);
    }

    /** Trả về giá trị Double an toàn, trả 0 nếu null. */
    private static double safe(Double v) {
        return v != null ? v : 0d;
    }

    /** Trả về giá trị phần trăm an toàn với giá trị mặc định nếu null. */
    private static double safePct(Double v, double fallback) {
        return v != null ? v : fallback;
    }
}
