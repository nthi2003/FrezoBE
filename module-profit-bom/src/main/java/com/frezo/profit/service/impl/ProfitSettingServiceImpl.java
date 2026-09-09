package com.frezo.profit.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.helper.SystemUtils;
import com.frezo.profit.dto.request.ProfitSettingRequest;
import com.frezo.profit.dto.response.ProfitSettingDto;
import com.frezo.profit.entity.ProfitSetting;
import com.frezo.profit.repository.ProfitSettingRepository;
import com.frezo.profit.service.ProfitSettingService;
import com.frezo.product.entity.Product;
import com.frezo.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfitSettingServiceImpl implements ProfitSettingService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String SCOPE_GLOBAL = "GLOBAL";
    private static final String SCOPE_CATEGORY = "CATEGORY";
    private static final String SCOPE_PRODUCT = "PRODUCT";

    private final ProfitSettingRepository repository;
    private final ProductRepository productRepository;
    private final JdbcTemplate jdbcTemplate;

    /** Liệt kê danh sách cấu hình lợi nhuận, có thể lọc theo phạm vi (GLOBAL/CATEGORY/PRODUCT). */
    @Override
    @Transactional(readOnly = true)
    public List<ProfitSettingDto> list(String scope) {
        List<ProfitSetting> rows = scope == null || scope.isBlank()
                ? repository.findByIsDeletedFalseOrderByScopeAscUpdatedDateDesc()
                : repository.findByScopeAndIsDeletedFalse(scope.toUpperCase());
        return rows.stream().map(this::toDto).toList();
    }

    /** Lấy chi tiết cấu hình lợi nhuận theo ID. */
    @Override
    @Transactional(readOnly = true)
    public ProfitSettingDto getById(String id) {
        return toDto(findActive(id));
    }

    /** Tạo mới cấu hình lợi nhuận, kiểm tra trùng phạm vi. */
    @Override
    @Transactional
    public ProfitSettingDto create(ProfitSettingRequest req) {
        String scope = normalizeScope(req.getScope());
        String scopeRefId = normalizeScopeRef(scope, req.getScopeRefId());
        repository.findByScopeAndScopeRefIdAndIsDeletedFalse(scope, scopeRefId)
                .ifPresent(x -> {
                    throw new AppException(CommonErrorCode.CONFLICT, "Cấu hình đã tồn tại cho phạm vi này");
                });
        ProfitSetting entity = mapToEntity(new ProfitSetting(), req, scope, scopeRefId);
        return toDto(repository.save(entity));
    }

    /** Cập nhật cấu hình lợi nhuận, kiểm tra trùng phạm vi với cấu hình khác. */
    @Override
    @Transactional
    public ProfitSettingDto update(String id, ProfitSettingRequest req) {
        ProfitSetting entity = findActive(id);
        String scope = normalizeScope(req.getScope());
        String scopeRefId = normalizeScopeRef(scope, req.getScopeRefId());
        repository.findByScopeAndScopeRefIdAndIsDeletedFalse(scope, scopeRefId)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(x -> {
                    throw new AppException(CommonErrorCode.CONFLICT, "Cấu hình đã tồn tại cho phạm vi này");
                });
        mapToEntity(entity, req, scope, scopeRefId);
        return toDto(repository.save(entity));
    }

    /** Xóa mềm cấu hình lợi nhuận. */
    @Override
    @Transactional
    public void delete(String id) {
        ProfitSetting entity = findActive(id);
        entity.softDelete(SystemUtils.getCurrentUsername());
        repository.save(entity);
    }

    /** Xác định cấu hình lợi nhuận áp dụng cho sản phẩm: ưu tiên PRODUCT > CATEGORY > GLOBAL. */
    @Override
    @Transactional(readOnly = true)
    public ProfitSettingDto resolveForProduct(String productId) {
        Product product = productRepository.findById(productId)
                .filter(p -> p.getIsDeleted() == null || !p.getIsDeleted())
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Không tìm thấy sản phẩm"));
        ProfitSetting resolved = resolveEntity(product).orElse(defaultGlobalSetting());
        return toDto(resolved);
    }

    /** Xác định entity cấu hình lợi nhuận theo thứ tự ưu tiên: PRODUCT → CATEGORY → GLOBAL. */
    Optional<ProfitSetting> resolveEntity(Product product) {
        Optional<ProfitSetting> productSetting = repository
                .findByScopeAndScopeRefIdAndIsDeletedFalse(SCOPE_PRODUCT, product.getId())
                .filter(s -> Boolean.TRUE.equals(s.getActive()));
        if (productSetting.isPresent()) {
            return productSetting;
        }
        if (product.getCategoryId() != null && !product.getCategoryId().isBlank()) {
            Optional<ProfitSetting> categorySetting = repository
                    .findByScopeAndScopeRefIdAndIsDeletedFalse(SCOPE_CATEGORY, product.getCategoryId())
                    .filter(s -> Boolean.TRUE.equals(s.getActive()));
            if (categorySetting.isPresent()) {
                return categorySetting;
            }
        }
        return repository.findByScopeAndScopeRefIdAndIsDeletedFalse(SCOPE_GLOBAL, null)
                .filter(s -> Boolean.TRUE.equals(s.getActive()));
    }

    /** Tạo cấu hình lợi nhuận mặc định khi database chưa có bản ghi. */
    private ProfitSetting defaultGlobalSetting() {
        return ProfitSetting.builder()
                .scope(SCOPE_GLOBAL)
                .scopeRefId(null)
                .name("Mặc định hệ thống")
                .profitMarginPct(20d)
                .riskCostPct(2d)
                .laborCostPerUnit(0d)
                .fixedMonthlyOverhead(0d)
                .monthlySalesTargetUnits(1000d)
                .defaultCogsPct(55d)
                .salesCommissionPct(3d)
                .corporateTaxPct(20d)
                .currency("VND")
                .active(true)
                .build();
    }

    /** Tìm cấu hình lợi nhuận active theo ID, ném ngoại lệ nếu không tồn tại. */
    private ProfitSetting findActive(String id) {
        return repository.findById(id)
                .filter(s -> s.getIsDeleted() == null || !s.getIsDeleted())
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Không tìm thấy cấu hình"));
    }

    /** Ánh xạ dữ liệu từ request vào entity cấu hình lợi nhuận. */
    private ProfitSetting mapToEntity(ProfitSetting entity, ProfitSettingRequest req, String scope, String scopeRefId) {
        entity.setScope(scope);
        entity.setScopeRefId(scopeRefId);
        entity.setName(req.getName());
        entity.setProfitMarginPct(defaultDouble(req.getProfitMarginPct(), 0d));
        entity.setRiskCostPct(defaultDouble(req.getRiskCostPct(), 0d));
        entity.setLaborCostPerUnit(defaultDouble(req.getLaborCostPerUnit(), 0d));
        entity.setFixedMonthlyOverhead(defaultDouble(req.getFixedMonthlyOverhead(), 0d));
        entity.setMonthlySalesTargetUnits(defaultDouble(req.getMonthlySalesTargetUnits(), 1000d));
        entity.setDefaultCogsPct(defaultDouble(req.getDefaultCogsPct(), 55d));
        entity.setSalesCommissionPct(defaultDouble(req.getSalesCommissionPct(), 3d));
        entity.setCorporateTaxPct(defaultDouble(req.getCorporateTaxPct(), 20d));
        entity.setCurrency(req.getCurrency() != null && !req.getCurrency().isBlank() ? req.getCurrency().trim().toUpperCase() : "VND");
        entity.setActive(req.getActive() == null || req.getActive());
        entity.setNote(req.getNote());
        return entity;
    }

    /** Chuyển entity cấu hình lợi nhuận sang DTO response kèm nhãn phạm vi. */
    private ProfitSettingDto toDto(ProfitSetting s) {
        return ProfitSettingDto.builder()
                .id(s.getId())
                .scope(s.getScope())
                .scopeRefId(s.getScopeRefId())
                .scopeLabel(resolveScopeLabel(s))
                .name(s.getName())
                .profitMarginPct(s.getProfitMarginPct())
                .riskCostPct(s.getRiskCostPct())
                .laborCostPerUnit(s.getLaborCostPerUnit())
                .fixedMonthlyOverhead(s.getFixedMonthlyOverhead())
                .monthlySalesTargetUnits(s.getMonthlySalesTargetUnits())
                .defaultCogsPct(s.getDefaultCogsPct())
                .salesCommissionPct(s.getSalesCommissionPct())
                .corporateTaxPct(s.getCorporateTaxPct())
                .currency(s.getCurrency())
                .active(Boolean.TRUE.equals(s.getActive()))
                .note(s.getNote())
                .updatedAt(s.getUpdatedDate() != null ? s.getUpdatedDate().format(ISO) : null)
                .build();
    }

    /** Xác định nhãn hiển thị cho phạm vi cấu hình (Toàn hệ thống / Danh mục / SP). */
    private String resolveScopeLabel(ProfitSetting s) {
        if (SCOPE_GLOBAL.equals(s.getScope())) {
            return "Toàn hệ thống";
        }
        if (SCOPE_CATEGORY.equals(s.getScope()) && s.getScopeRefId() != null) {
            try {
                String name = jdbcTemplate.queryForObject(
                        "SELECT name FROM categories WHERE id = ? LIMIT 1", String.class, s.getScopeRefId());
                return "Danh mục: " + name;
            } catch (Exception ex) {
                return "Danh mục: " + s.getScopeRefId();
            }
        }
        if (SCOPE_PRODUCT.equals(s.getScope()) && s.getScopeRefId() != null) {
            return productRepository.findById(s.getScopeRefId())
                    .map(p -> "SP: " + p.getName())
                    .orElse("SP: " + s.getScopeRefId());
        }
        return s.getScope();
    }

    /** Chuẩn hóa phạm vi: trim, uppercase, kiểm tra bắt buộc. */
    private static String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "scope bắt buộc (GLOBAL|CATEGORY|PRODUCT)");
        }
        return scope.trim().toUpperCase();
    }

    /** Chuẩn hóa scopeRefId: null nếu GLOBAL, trim nếu CATEGORY/PRODUCT. */
    private static String normalizeScopeRef(String scope, String scopeRefId) {
        if (SCOPE_GLOBAL.equals(scope)) {
            return null;
        }
        if (scopeRefId == null || scopeRefId.isBlank()) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "scopeRefId bắt buộc với CATEGORY/PRODUCT");
        }
        return scopeRefId.trim();
    }

    /** Trả về giá trị Double với giá trị mặc định nếu null. */
    private static double defaultDouble(Double v, double fallback) {
        return v != null ? v : fallback;
    }
}
