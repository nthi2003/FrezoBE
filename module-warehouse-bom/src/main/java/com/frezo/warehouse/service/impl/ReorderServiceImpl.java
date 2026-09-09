package com.frezo.warehouse.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.response.FePage;
import com.frezo.product.entity.Product;
import com.frezo.product.repository.ProductRepository;
import com.frezo.warehouse.dto.request.ReorderRuleRequest;
import com.frezo.warehouse.dto.response.ReorderRuleDto;
import com.frezo.warehouse.dto.response.StockAlertDto;
import com.frezo.warehouse.dto.response.WarehouseOptionDto;
import com.frezo.warehouse.entity.ReorderRule;
import com.frezo.warehouse.entity.StockAlert;
import com.frezo.warehouse.entity.Warehouse;
import com.frezo.warehouse.repository.ReorderRuleRepository;
import com.frezo.warehouse.repository.StockAlertRepository;
import com.frezo.warehouse.repository.StockBalanceRepository;
import com.frezo.warehouse.repository.StockBatchRepository;
import com.frezo.warehouse.repository.WarehouseRepository;
import com.frezo.warehouse.service.ReorderService;
import com.frezo.warehouse.service.StockAlertNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reorder rules + stock alerts. Enrich product/warehouse inline (≤5 deps).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReorderServiceImpl implements ReorderService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ReorderRuleRepository ruleRepository;
    private final StockAlertRepository alertRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockBatchRepository stockBatchRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final JdbcTemplate jdbcTemplate;
    private final StockAlertNotifier stockAlertNotifier;

    /**
     * Liệt kê danh sách kho chưa xóa dưới dạng các lựa chọn cho giao diện.
     */
    @Override
    public List<WarehouseOptionDto> listWarehouses() {
        return warehouseRepository.findAll().stream()
                .filter(w -> Boolean.FALSE.equals(w.getIsDeleted()) || w.getIsDeleted() == null)
                .map(w -> WarehouseOptionDto.builder()
                        .id(w.getId()).name(w.getName()).code(w.getCode()).build())
                .toList();
    }

    /**
     * Lọc danh sách quy tắc đặt hàng lại theo kho và sản phẩm, sắp xếp theo thời gian cập nhật giảm dần.
     */
    @Override
    public FePage<ReorderRuleDto> listRules(String warehouseId, String productId) {
        List<ReorderRuleDto> list = ruleRepository.findByIsDeletedFalseOrderByUpdatedDateDesc().stream()
                .filter(r -> warehouseId == null || warehouseId.equals(r.getWarehouseId()))
                .filter(r -> productId == null || productId.equals(r.getProductId()))
                .map(this::toRuleDto)
                .toList();
        return FePage.all(list);
    }

    /**
     * Tạo mới quy tắc đặt hàng lại cho một cặp sản phẩm + kho (báo lỗi nếu quy tắc đã tồn tại).
     */
    @Override
    @Transactional
    public ReorderRuleDto create(ReorderRuleRequest req) {
        ruleRepository.findByProductIdAndWarehouseIdAndIsDeletedFalse(req.getProductId(), req.getWarehouseId())
                .ifPresent(x -> {
                    throw new AppException(CommonErrorCode.CONFLICT, "Rule đã tồn tại cho product+warehouse");
                });
        ReorderRule r = ReorderRule.builder()
                .warehouseId(req.getWarehouseId())
                .productId(req.getProductId())
                .minQty(req.getMinQty())
                .maxQty(req.getMaxQty())
                .reorderQty(req.getReorderQty())
                .active(req.getActive() == null || req.getActive())
                .build();
        r.setId(UUID.randomUUID().toString());
        return toRuleDto(ruleRepository.save(r));
    }

    /**
     * Cập nhật các trường của quy tắc đặt hàng lại theo yêu cầu (chỉ cập nhật những trường được cung cấp).
     */
    @Override
    @Transactional
    public ReorderRuleDto update(String id, ReorderRuleRequest req) {
        ReorderRule r = ruleRepository.findById(id)
                .filter(x -> Boolean.FALSE.equals(x.getIsDeleted()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Reorder rule không tồn tại"));
        if (req.getMinQty() != null) r.setMinQty(req.getMinQty());
        if (req.getMaxQty() != null) r.setMaxQty(req.getMaxQty());
        if (req.getReorderQty() != null) r.setReorderQty(req.getReorderQty());
        if (req.getActive() != null) r.setActive(req.getActive());
        if (req.getWarehouseId() != null) r.setWarehouseId(req.getWarehouseId());
        if (req.getProductId() != null) r.setProductId(req.getProductId());
        return toRuleDto(ruleRepository.save(r));
    }

    /**
     * Xóa mềm (soft delete) một quy tắc đặt hàng lại theo id.
     */
    @Override
    @Transactional
    public void delete(String id) {
        ReorderRule r = ruleRepository.findById(id)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Reorder rule không tồn tại"));
        r.softDelete(SystemUtils.getCurrentUsername());
        ruleRepository.save(r);
    }

    /**
     * Xử lý import quy tắc từ file Excel (hiện là stub MVP, trả về kết quả rỗng).
     */
    @Override
    public Map<String, Integer> importExcel(MultipartFile file) {
        // MVP stub — FE wizard có thể parse client-side; BE trả 0 để không 501.
        log.info("[Reorder] import-excel stub, file={}", file != null ? file.getOriginalFilename() : null);
        Map<String, Integer> result = new HashMap<>();
        result.put("imported", 0);
        return result;
    }

    /**
     * Lọc danh sách cảnh báo tồn kho theo trạng thái và loại, phân trang, kèm thông tin kho/sản phẩm/lô.
     */
    @Override
    public FePage<StockAlertDto> listAlerts(String status, String alertType, int page, int size) {
        String st = (status == null || status.isBlank() || "all".equalsIgnoreCase(status))
                ? null : status.toUpperCase();
        String at = (alertType == null || alertType.isBlank())
                ? null : alertType.toUpperCase();

        List<StockAlert> alerts = alertRepository.findFiltered(st, at);

        Set<String> whIds = alerts.stream().map(StockAlert::getWarehouseId).collect(Collectors.toSet());
        Set<String> prIds = alerts.stream().map(StockAlert::getProductId).collect(Collectors.toSet());
        Set<String> btIds = alerts.stream().map(StockAlert::getBatchId).filter(b -> b != null).collect(Collectors.toSet());

        Map<String, Warehouse> whMap = whIds.isEmpty() ? Map.of()
                : warehouseRepository.findAllById(whIds).stream()
                .collect(Collectors.toMap(Warehouse::getId, w -> w));
        Map<String, Product> prMap = prIds.isEmpty() ? Map.of()
                : productRepository.findAllById(prIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Map<String, com.frezo.warehouse.entity.StockBatch> btMap = btIds.isEmpty() ? Map.of()
                : stockBatchRepository.findAllById(btIds).stream()
                .collect(Collectors.toMap(com.frezo.warehouse.entity.StockBatch::getId, b -> b));

        List<StockAlertDto> dtos = alerts.stream()
                .map(a -> toAlertDto(a, whMap, prMap, btMap))
                .toList();

        return FePage.of(dtos, page, size);
    }

    /**
     * Đánh dấu một cảnh báo tồn kho là đã xem xét (DISMISSED) và ghi nhận thời điểm xem xét.
     */
    @Override
    @Transactional
    public StockAlertDto dismiss(String id) {
        StockAlert a = alertRepository.findById(id)
                .filter(x -> Boolean.FALSE.equals(x.getIsDeleted()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Alert không tồn tại"));
        a.setStatus("DISMISSED");
        a.setDismissedAt(LocalDateTime.now());
        return toAlertDto(alertRepository.save(a));
    }

    /**
     * Quét các quy tắc đặt hàng lại đang hoạt động, phát hiện sản phẩm dưới ngưỡng minQty,
     * tạo cảnh báo LOW_STOCK duy nhất theo ngày và gửi thông báo.
     */
    @Override
    @Transactional
    public void scanAndRaiseAlerts() {
        List<ReorderRule> rules = ruleRepository.findByActiveTrueAndIsDeletedFalse();
        String dayKey = LocalDate.now().toString().replace("-", "");
        int raised = 0;
        for (ReorderRule rule : rules) {
            Double qty = stockBalanceRepository
                    .sumAvailableByProductAndWarehouse(rule.getProductId(), rule.getWarehouseId());
            double current = qty != null ? qty : 0.0;
            double min = rule.getMinQty() != null ? rule.getMinQty() : 0.0;
            if (current >= min) continue;

            String idem = rule.getProductId() + "|" + rule.getWarehouseId() + "|" + dayKey;
            if (alertRepository.existsByIdempotencyKeyAndIsDeletedFalse(idem)) continue;
            if (alertRepository.existsOpenAlertOfType(
                    rule.getProductId(), rule.getWarehouseId(), "LOW_STOCK", "OPEN")) continue;

            String severity = current <= 0 ? "CRITICAL" : "WARNING";
            StockAlert alert = StockAlert.builder()
                    .warehouseId(rule.getWarehouseId())
                    .productId(rule.getProductId())
                    .currentQty(current)
                    .minQty(min)
                    .severity(severity)
                    .status("OPEN")
                    .triggeredAt(LocalDateTime.now())
                    .idempotencyKey(idem)
                    .alertType("LOW_STOCK")
                    .build();
            alert.setId(UUID.randomUUID().toString());
            alertRepository.save(alert);
            stockAlertNotifier.notifyAlert(alert);
            raised++;
        }
        log.info("[Reorder] scan done — raised {} alerts from {} rules", raised, rules.size());
    }

    /**
     * Chuyển một quy tắc đặt hàng lại thành DTO phản hồi kèm tên kho, sản phẩm và danh mục.
     */
    private ReorderRuleDto toRuleDto(ReorderRule r) {
        Warehouse wh = warehouseRepository.findById(r.getWarehouseId()).orElse(null);
        Product p = productRepository.findById(r.getProductId()).orElse(null);
        return ReorderRuleDto.builder()
                .id(r.getId())
                .warehouseId(r.getWarehouseId())
                .warehouseName(wh != null ? wh.getName() : null)
                .productId(r.getProductId())
                .productCode(p != null ? p.getCode() : null)
                .productName(p != null ? p.getName() : null)
                .categoryName(p != null ? resolveCategoryName(p.getCategoryId()) : null)
                .minQty(r.getMinQty())
                .maxQty(r.getMaxQty())
                .reorderQty(r.getReorderQty())
                .active(Boolean.TRUE.equals(r.getActive()))
                .updatedAt(r.getUpdatedDate() != null ? r.getUpdatedDate().format(ISO) : null)
                .build();
    }

    /**
     * Tra tên danh mục theo id từ bảng categories; trả null nếu không tìm thấy.
     */
    private String resolveCategoryName(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT name FROM categories
                    WHERE id = ? AND COALESCE(is_deleted, false) = false
                    LIMIT 1
                    """,
                    String.class,
                    categoryId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    /**
     * Chuyển một cảnh báo thành DTO phản hồi, tra cứu kho, sản phẩm và mã lô trực tiếp từ repository.
     */
    private StockAlertDto toAlertDto(StockAlert a) {
        Warehouse wh = warehouseRepository.findById(a.getWarehouseId()).orElse(null);
        Product p = productRepository.findById(a.getProductId()).orElse(null);
        String batchCode = null;
        if (a.getBatchId() != null) {
            batchCode = stockBatchRepository.findById(a.getBatchId())
                    .map(b -> b.getBatchCode())
                    .orElse(null);
        }
        return buildAlertDto(a, wh, p, batchCode);
    }

    /**
     * Chuyển một cảnh báo thành DTO phản hồi dùng các bản đồ tra cứu kho/sản phẩm/lô đã nạp sẵn.
     */
    private StockAlertDto toAlertDto(StockAlert a,
                                     Map<String, Warehouse> whMap,
                                     Map<String, Product> prMap,
                                     Map<String, com.frezo.warehouse.entity.StockBatch> btMap) {
        Warehouse wh = whMap.get(a.getWarehouseId());
        Product p = prMap.get(a.getProductId());
        String batchCode = null;
        if (a.getBatchId() != null) {
            com.frezo.warehouse.entity.StockBatch bt = btMap.get(a.getBatchId());
            batchCode = bt != null ? bt.getBatchCode() : null;
        }
        return buildAlertDto(a, wh, p, batchCode);
    }

    /**
     * Dựng DTO phản hồi cảnh báo từ dữ liệu cảnh báo cùng thông tin kho, sản phẩm và mã lô.
     */
    private StockAlertDto buildAlertDto(StockAlert a, Warehouse wh, Product p, String batchCode) {
        return StockAlertDto.builder()
                .id(a.getId())
                .warehouseId(a.getWarehouseId())
                .warehouseName(wh != null ? wh.getName() : null)
                .productId(a.getProductId())
                .productCode(p != null ? p.getCode() : null)
                .productName(p != null ? p.getName() : null)
                .categoryName(p != null ? resolveCategoryName(p.getCategoryId()) : null)
                .currentQty(a.getCurrentQty())
                .minQty(a.getMinQty())
                .severity(a.getSeverity())
                .status(a.getStatus())
                .triggeredAt(a.getTriggeredAt() != null ? a.getTriggeredAt().format(ISO) : null)
                .dismissedAt(a.getDismissedAt() != null ? a.getDismissedAt().format(ISO) : null)
                .alertType(a.getAlertType() != null ? a.getAlertType() : "LOW_STOCK")
                .batchId(a.getBatchId())
                .batchCode(batchCode)
                .expiryDate(a.getExpiryDate() != null ? a.getExpiryDate().toString() : null)
                .daysToExpiry(a.getDaysToExpiry())
                .build();
    }
}
