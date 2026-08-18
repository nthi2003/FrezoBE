package com.frezo.profit.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.helper.SystemUtils;
import com.frezo.profit.common.CostCategoryCode;
import com.frezo.profit.common.Salary3P;
import com.frezo.profit.dto.request.ProfitCostCategoryRequest;
import com.frezo.profit.dto.request.ProfitEmployeeAllocationRequest;
import com.frezo.profit.dto.request.ProfitFixedOverheadRequest;
import com.frezo.profit.dto.request.ProfitSalesTargetPreviewRequest;
import com.frezo.profit.dto.response.ProfitCostCategoryDto;
import com.frezo.profit.dto.response.ProfitEmployeeAllocationDto;
import com.frezo.profit.dto.response.ProfitFixedOverheadDto;
import com.frezo.profit.dto.response.ProfitSalesTargetPreviewDto;
import com.frezo.profit.entity.ProfitCostCategory;
import com.frezo.profit.entity.ProfitEmployeeAllocation;
import com.frezo.profit.entity.ProfitFixedOverhead;
import com.frezo.profit.entity.ProfitSetting;
import com.frezo.profit.repository.ProfitCostCategoryRepository;
import com.frezo.profit.repository.ProfitEmployeeAllocationRepository;
import com.frezo.profit.repository.ProfitFixedOverheadRepository;
import com.frezo.profit.repository.ProfitSettingRepository;
import com.frezo.profit.service.ProfitCostStructureService;
import com.frezo.profit.service.support.ProfitContractSalaryResolver;
import com.frezo.qlns.entity.Contract;
import com.frezo.qlns.entity.Payroll;
import com.frezo.qlns.repository.PayrollRepository;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfitCostStructureServiceImpl implements ProfitCostStructureService {

    private static final Map<CostCategoryCode, String> CATEGORY_LABELS = Map.of(
            CostCategoryCode.SALES_VARIABLE, "Nhân viên kinh doanh",
            CostCategoryCode.FIXED_IT, "IT / Hệ thống",
            CostCategoryCode.FIXED_LOGISTICS, "Vận chuyển / Logistics",
            CostCategoryCode.FIXED_QC, "Kiểm định / QC",
            CostCategoryCode.FIXED_ADMIN, "Hành chính",
            CostCategoryCode.VARIABLE_RISK, "Chi phí rủi ro",
            CostCategoryCode.VARIABLE_COMMISSION, "Hoa hồng bán hàng"
    );

    private final ProfitCostCategoryRepository categoryRepository;
    private final ProfitFixedOverheadRepository overheadRepository;
    private final ProfitEmployeeAllocationRepository allocationRepository;
    private final PersonRepository personRepository;
    private final PayrollRepository payrollRepository;
    private final ProfitSettingRepository profitSettingRepository;
    private final ProfitContractSalaryResolver contractSalaryResolver;

    @Override
    @Transactional(readOnly = true)
    public List<ProfitCostCategoryDto> listCategories(String marketCode) {
        List<ProfitCostCategory> rows = marketCode == null || marketCode.isBlank()
                ? categoryRepository.findByIsDeletedFalseOrderBySortOrderAscNameAsc()
                : categoryRepository.findByMarketCodeAndIsDeletedFalseOrderBySortOrderAsc(marketCode.toUpperCase());
        return rows.stream().map(this::toCategoryDto).toList();
    }

    @Override
    @Transactional
    public ProfitCostCategoryDto createCategory(ProfitCostCategoryRequest req) {
        String market = req.getMarketCode() != null ? req.getMarketCode().trim().toUpperCase() : "VN";
        categoryRepository.findByCodeAndMarketCodeAndIsDeletedFalse(req.getCode(), market)
                .ifPresent(x -> {
                    throw new AppException(CommonErrorCode.CONFLICT, "Danh mục chi phí đã tồn tại");
                });
        ProfitCostCategory entity = mapCategory(new ProfitCostCategory(), req, market);
        return toCategoryDto(categoryRepository.save(entity));
    }

    @Override
    @Transactional
    public ProfitCostCategoryDto updateCategory(String id, ProfitCostCategoryRequest req) {
        ProfitCostCategory entity = findCategory(id);
        String market = req.getMarketCode() != null ? req.getMarketCode().trim().toUpperCase() : entity.getMarketCode();
        categoryRepository.findByCodeAndMarketCodeAndIsDeletedFalse(req.getCode(), market)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(x -> {
                    throw new AppException(CommonErrorCode.CONFLICT, "Danh mục chi phí đã tồn tại");
                });
        mapCategory(entity, req, market);
        return toCategoryDto(categoryRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteCategory(String id) {
        ProfitCostCategory entity = findCategory(id);
        entity.softDelete(SystemUtils.getCurrentUsername());
        categoryRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfitFixedOverheadDto> listFixedOverhead(Integer year, Integer month) {
        int y = year != null ? year : YearMonth.now().getYear();
        int m = month != null ? month : YearMonth.now().getMonthValue();
        Map<String, String> names = categoryRepository.findByIsDeletedFalseOrderBySortOrderAscNameAsc().stream()
                .collect(Collectors.toMap(ProfitCostCategory::getId, ProfitCostCategory::getName, (a, b) -> a));
        return overheadRepository.findByYearAndMonthAndIsDeletedFalse(y, m).stream()
                .map(o -> toOverheadDto(o, names.get(o.getCategoryId())))
                .toList();
    }

    @Override
    @Transactional
    public ProfitFixedOverheadDto upsertFixedOverhead(ProfitFixedOverheadRequest req) {
        findCategory(req.getCategoryId());
        ProfitFixedOverhead entity = overheadRepository
                .findByCategoryIdAndYearAndMonthAndIsDeletedFalse(req.getCategoryId(), req.getYear(), req.getMonth())
                .orElseGet(ProfitFixedOverhead::new);
        entity.setCategoryId(req.getCategoryId());
        entity.setYear(req.getYear());
        entity.setMonth(req.getMonth());
        entity.setAmount(req.getAmount());
        entity.setCurrency(req.getCurrency() != null ? req.getCurrency().trim().toUpperCase() : "VND");
        entity.setNote(req.getNote());
        ProfitFixedOverhead saved = overheadRepository.save(entity);
        String catName = findCategory(saved.getCategoryId()).getName();
        return toOverheadDto(saved, catName);
    }

    @Override
    @Transactional
    public void deleteFixedOverhead(String id) {
        ProfitFixedOverhead entity = overheadRepository.findById(id)
                .filter(o -> o.getIsDeleted() == null || !o.getIsDeleted())
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Không tìm thấy chi phí cố định"));
        entity.softDelete(SystemUtils.getCurrentUsername());
        overheadRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfitEmployeeAllocationDto> listEmployeeAllocations() {
        YearMonth now = YearMonth.now();
        return allocationRepository.findByIsDeletedFalseOrderByCostCategoryCodeAsc().stream()
                .map(a -> toAllocationDto(a, now.getYear(), now.getMonthValue()))
                .toList();
    }

    @Override
    @Transactional
    public ProfitEmployeeAllocationDto createEmployeeAllocation(ProfitEmployeeAllocationRequest req) {
        validatePerson(req.getPersonId());
        ProfitEmployeeAllocation entity = mapAllocation(new ProfitEmployeeAllocation(), req);
        YearMonth now = YearMonth.now();
        return toAllocationDto(allocationRepository.save(entity), now.getYear(), now.getMonthValue());
    }

    @Override
    @Transactional
    public ProfitEmployeeAllocationDto updateEmployeeAllocation(String id, ProfitEmployeeAllocationRequest req) {
        ProfitEmployeeAllocation entity = findAllocation(id);
        validatePerson(req.getPersonId());
        mapAllocation(entity, req);
        YearMonth now = YearMonth.now();
        return toAllocationDto(allocationRepository.save(entity), now.getYear(), now.getMonthValue());
    }

    @Override
    @Transactional
    public void deleteEmployeeAllocation(String id) {
        ProfitEmployeeAllocation entity = findAllocation(id);
        entity.softDelete(SystemUtils.getCurrentUsername());
        allocationRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfitSalesTargetPreviewDto previewSalesTarget(ProfitSalesTargetPreviewRequest req) {
        validatePerson(req.getPersonId());
        ProfitSetting global = resolveGlobalSetting();
        Salary3P salary = contractSalaryResolver.resolve3P(req.getPersonId(), req.getContractId());
        var contractOpt = contractSalaryResolver.findActiveContract(req.getPersonId(), req.getContractId());

        double cogs = pct(global.getDefaultCogsPct(), 55d);
        double risk = pct(global.getRiskCostPct(), 2d);
        double comm = req.getCommissionPct() != null
                ? req.getCommissionPct()
                : pct(global.getSalesCommissionPct(), 3d);
        double margin = Salary3P.contributionMarginPct(cogs, risk, comm);
        double target = contractSalaryResolver.resolveRevenueTarget(salary, global, req.getCommissionPct());

        return ProfitSalesTargetPreviewDto.builder()
                .personId(req.getPersonId())
                .contractId(contractOpt.map(Contract::getId).orElse(req.getContractId()))
                .contractCode(contractOpt.map(Contract::getCode).orElse(null))
                .salaryP1(salary.p1Position())
                .salaryP2(salary.p2Person())
                .salaryP3(salary.p3Performance())
                .salaryFixed(salary.fixedMonthly())
                .salaryTotalAtFullKpi(salary.totalAtFullKpi())
                .cogsPct(cogs)
                .riskPct(risk)
                .commissionPct(comm)
                .contributionMarginPct(margin)
                .calculatedRevenueTarget(target)
                .currency(global.getCurrency() != null ? global.getCurrency() : "VND")
                .fromContract(contractOpt.isPresent())
                .note(contractOpt.isPresent()
                        ? "Tính từ HĐ 3P — doanh thu cần đạt để cover P1+P2+P3 sau trừ COGS/rủi ro/HH"
                        : "Chưa có HĐ — nhập lương thủ công hoặc liên kết hợp đồng")
                .build();
    }

    private ProfitSetting resolveGlobalSetting() {
        return profitSettingRepository.findByScopeAndScopeRefIdAndIsDeletedFalse("GLOBAL", null)
                .orElseGet(() -> ProfitSetting.builder()
                        .defaultCogsPct(55d)
                        .riskCostPct(2d)
                        .salesCommissionPct(3d)
                        .corporateTaxPct(20d)
                        .currency("VND")
                        .build());
    }

    private static double pct(Double v, double fallback) {
        return v != null ? v : fallback;
    }

    private ProfitCostCategory findCategory(String id) {
        return categoryRepository.findById(id)
                .filter(c -> c.getIsDeleted() == null || !c.getIsDeleted())
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Không tìm thấy danh mục chi phí"));
    }

    private ProfitEmployeeAllocation findAllocation(String id) {
        return allocationRepository.findById(id)
                .filter(a -> a.getIsDeleted() == null || !a.getIsDeleted())
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Không tìm thấy phân bổ nhân sự"));
    }

    private void validatePerson(String personId) {
        personRepository.findById(personId)
                .filter(p -> p.getIsDeleted() == null || !p.getIsDeleted())
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Không tìm thấy nhân viên"));
    }

    private ProfitCostCategory mapCategory(ProfitCostCategory entity, ProfitCostCategoryRequest req, String market) {
        entity.setCode(req.getCode());
        entity.setName(req.getName());
        entity.setNameEn(req.getNameEn());
        entity.setCostType(req.getCostType());
        entity.setCurrency(req.getCurrency() != null ? req.getCurrency().trim().toUpperCase() : "VND");
        entity.setMarketCode(market);
        entity.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        entity.setActive(req.getActive() == null || req.getActive());
        entity.setNote(req.getNote());
        return entity;
    }

    private ProfitEmployeeAllocation mapAllocation(ProfitEmployeeAllocation entity, ProfitEmployeeAllocationRequest req) {
        entity.setPersonId(req.getPersonId());
        entity.setContractId(req.getContractId());
        entity.setCostCategoryCode(req.getCostCategoryCode());
        entity.setMonthlySalary(req.getMonthlySalary());
        entity.setUsePayrollSource(req.getUsePayrollSource() != null && req.getUsePayrollSource());
        entity.setAutoCalculateTarget(req.getAutoCalculateTarget() == null || req.getAutoCalculateTarget());
        entity.setCommissionPct(req.getCommissionPct());
        entity.setCurrency(req.getCurrency() != null ? req.getCurrency().trim().toUpperCase() : "VND");
        entity.setActive(req.getActive() == null || req.getActive());
        entity.setNote(req.getNote());

        if (Boolean.TRUE.equals(entity.getAutoCalculateTarget())
                && entity.getCostCategoryCode() == CostCategoryCode.SALES_VARIABLE) {
            ProfitSetting global = resolveGlobalSetting();
            Salary3P salary = contractSalaryResolver.resolve3P(req.getPersonId(), req.getContractId());
            double target = contractSalaryResolver.resolveRevenueTarget(salary, global, req.getCommissionPct());
            entity.setRevenueTargetMonthly(target);
            if (entity.getMonthlySalary() == null && salary.fixedMonthly() > 0) {
                entity.setMonthlySalary(salary.fixedMonthly());
            }
        } else {
            entity.setRevenueTargetMonthly(req.getRevenueTargetMonthly());
        }
        return entity;
    }

    private ProfitCostCategoryDto toCategoryDto(ProfitCostCategory c) {
        return ProfitCostCategoryDto.builder()
                .id(c.getId())
                .code(c.getCode())
                .name(c.getName())
                .nameEn(c.getNameEn())
                .costType(c.getCostType())
                .currency(c.getCurrency())
                .marketCode(c.getMarketCode())
                .sortOrder(c.getSortOrder())
                .active(Boolean.TRUE.equals(c.getActive()))
                .note(c.getNote())
                .build();
    }

    private ProfitFixedOverheadDto toOverheadDto(ProfitFixedOverhead o, String categoryName) {
        return ProfitFixedOverheadDto.builder()
                .id(o.getId())
                .categoryId(o.getCategoryId())
                .categoryName(categoryName)
                .year(o.getYear())
                .month(o.getMonth())
                .amount(o.getAmount())
                .currency(o.getCurrency())
                .note(o.getNote())
                .build();
    }

    private ProfitEmployeeAllocationDto toAllocationDto(ProfitEmployeeAllocation a, int year, int month) {
        double resolved = resolveSalary(a, year, month);
        Salary3P salary = contractSalaryResolver.resolve3P(a.getPersonId(), a.getContractId());
        ProfitSetting global = resolveGlobalSetting();
        Double calculatedTarget = Boolean.TRUE.equals(a.getAutoCalculateTarget())
                ? contractSalaryResolver.resolveRevenueTarget(salary, global, a.getCommissionPct())
                : a.getRevenueTargetMonthly();
        return ProfitEmployeeAllocationDto.builder()
                .id(a.getId())
                .personId(a.getPersonId())
                .personName(resolvePersonName(a.getPersonId()))
                .contractId(a.getContractId())
                .costCategoryCode(a.getCostCategoryCode())
                .costCategoryLabel(CATEGORY_LABELS.getOrDefault(a.getCostCategoryCode(), a.getCostCategoryCode().name()))
                .monthlySalary(a.getMonthlySalary())
                .usePayrollSource(Boolean.TRUE.equals(a.getUsePayrollSource()))
                .resolvedSalary(resolved)
                .revenueTargetMonthly(a.getRevenueTargetMonthly())
                .commissionPct(a.getCommissionPct())
                .autoCalculateTarget(Boolean.TRUE.equals(a.getAutoCalculateTarget()))
                .salaryP1(salary.p1Position())
                .salaryP2(salary.p2Person())
                .salaryP3(salary.p3Performance())
                .calculatedRevenueTarget(calculatedTarget)
                .currency(a.getCurrency())
                .active(Boolean.TRUE.equals(a.getActive()))
                .note(a.getNote())
                .build();
    }

    private double resolveSalary(ProfitEmployeeAllocation alloc, int year, int month) {
        if (Boolean.TRUE.equals(alloc.getUsePayrollSource())) {
            return payrollRepository
                    .findByPersonIdAndMonthAndYearAndIsDeletedFalseOrderByUpdatedDateDescIdDesc(
                            alloc.getPersonId(), month, year)
                    .stream()
                    .findFirst()
                    .map(Payroll::getGrossSalary)
                    .map(BigDecimal::doubleValue)
                    .orElse(alloc.getMonthlySalary() != null ? alloc.getMonthlySalary() : 0d);
        }
        return alloc.getMonthlySalary() != null ? alloc.getMonthlySalary() : 0d;
    }

    private String resolvePersonName(String personId) {
        return personRepository.findById(personId)
                .filter(p -> p.getIsDeleted() == null || !p.getIsDeleted())
                .map(Person::getName)
                .orElse(personId);
    }
}
