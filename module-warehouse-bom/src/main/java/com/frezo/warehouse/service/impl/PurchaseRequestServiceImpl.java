package com.frezo.warehouse.service.impl;

import com.frezo.approval.entity.ApprovalRequest;
import com.frezo.approval.service.ApprovalCreator;
import com.frezo.common.domain.SubjectType;
import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.response.FePage;
import com.frezo.warehouse.dto.request.FromAlertsRequest;
import com.frezo.warehouse.dto.request.PurchaseRequestSaveRequest;
import com.frezo.warehouse.dto.response.PurchaseRequestDto;
import com.frezo.warehouse.dto.response.PurchaseRequestLineDto;
import com.frezo.warehouse.entity.PurchaseRequest;
import com.frezo.warehouse.entity.PurchaseRequestLine;
import com.frezo.warehouse.entity.ReorderRule;
import com.frezo.warehouse.entity.StockAlert;
import com.frezo.warehouse.entity.Warehouse;
import com.frezo.warehouse.repository.PurchaseRequestLineRepository;
import com.frezo.warehouse.repository.PurchaseRequestRepository;
import com.frezo.warehouse.repository.ReorderRuleRepository;
import com.frezo.warehouse.repository.StockAlertRepository;
import com.frezo.warehouse.repository.WarehouseRepository;
import com.frezo.warehouse.service.PurchaseRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseRequestServiceImpl implements PurchaseRequestService {

    public static final String FLOW_CODE = "PURCHASE_REQUEST";
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PurchaseRequestRepository prRepository;
    private final PurchaseRequestLineRepository lineRepository;
    private final StockAlertRepository alertRepository;
    private final ReorderRuleRepository reorderRuleRepository;
    private final ApprovalCreator approvalCreator;
    private final WarehouseRepository warehouseRepository;
    private final JdbcTemplate jdbcTemplate;

    /** LNK-05: true = bắt buộc flow PURCHASE_REQUEST; false = bypass DRAFT→APPROVED + badge. */
    @Value("${warehouse.pr.approval.required:true}")
    private boolean approvalRequired;

    @Override
    @Transactional
    public List<PurchaseRequestDto> createFromAlerts(FromAlertsRequest req) {
        if (req == null || req.getAlertIds() == null || req.getAlertIds().isEmpty()) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "alertIds bắt buộc");
        }
        List<StockAlert> alerts = new ArrayList<>();
        for (String aid : req.getAlertIds()) {
            StockAlert a = alertRepository.findById(aid)
                    .filter(x -> Boolean.FALSE.equals(x.getIsDeleted()) || x.getIsDeleted() == null)
                    .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Alert " + aid));
            if (a.getPurchaseRequestId() != null && !a.getPurchaseRequestId().isBlank()) {
                throw new AppException(CommonErrorCode.CONFLICT,
                        "Alert " + aid + " đã gắn PR " + a.getPurchaseRequestId());
            }
            if (lineRepository.existsByStockAlertIdAndIsDeletedFalse(aid)) {
                throw new AppException(CommonErrorCode.CONFLICT,
                        "Alert " + aid + " đã có dòng PR active");
            }
            alerts.add(a);
        }

        // Group by supplier (from ReorderRule.preferredSupplierId) + warehouse
        Map<String, List<StockAlert>> groups = new LinkedHashMap<>();
        Map<String, String> groupSupplier = new LinkedHashMap<>();
        Map<String, String> groupWarehouse = new LinkedHashMap<>();
        for (StockAlert a : alerts) {
            ReorderRule rule = reorderRuleRepository
                    .findByProductIdAndWarehouseIdAndIsDeletedFalse(a.getProductId(), a.getWarehouseId())
                    .orElse(null);
            String supplier = rule != null && rule.getPreferredSupplierId() != null
                    ? rule.getPreferredSupplierId() : "NONE";
            String key = supplier + "|" + a.getWarehouseId();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(a);
            groupSupplier.put(key, "NONE".equals(supplier) ? null : supplier);
            groupWarehouse.put(key, a.getWarehouseId());
        }

        List<PurchaseRequestDto> created = new ArrayList<>();
        for (Map.Entry<String, List<StockAlert>> e : groups.entrySet()) {
            PurchaseRequest pr = PurchaseRequest.builder()
                    .code(nextCode())
                    .supplierId(groupSupplier.get(e.getKey()))
                    .warehouseId(groupWarehouse.get(e.getKey()))
                    .status("DRAFT")
                    .note("Sinh từ StockAlert")
                    .build();
            pr.setId(UUID.randomUUID().toString());
            pr = prRepository.save(pr);

            for (StockAlert a : e.getValue()) {
                ReorderRule rule = reorderRuleRepository
                        .findByProductIdAndWarehouseIdAndIsDeletedFalse(a.getProductId(), a.getWarehouseId())
                        .orElse(null);
                double qty = resolveQty(rule, a);
                PurchaseRequestLine line = PurchaseRequestLine.builder()
                        .purchaseRequestId(pr.getId())
                        .productId(a.getProductId())
                        .warehouseId(a.getWarehouseId())
                        .qty(qty)
                        .stockAlertId(a.getId())
                        .build();
                line.setId(UUID.randomUUID().toString());
                lineRepository.save(line);

                a.setPurchaseRequestId(pr.getId());
                alertRepository.save(a);
            }
            created.add(toDto(pr));
        }
        return created;
    }

    @Override
    public FePage<PurchaseRequestDto> list() {
        return FePage.all(prRepository.findByIsDeletedFalseOrderByCreatedDateDesc().stream()
                .map(this::toDto).toList());
    }

    @Override
    public PurchaseRequestDto get(String id) {
        return toDto(find(id));
    }

    @Override
    @Transactional
    public PurchaseRequestDto create(PurchaseRequestSaveRequest req) {
        PurchaseRequest pr = PurchaseRequest.builder()
                .code(nextCode())
                .supplierId(req.getSupplierId())
                .warehouseId(req.getWarehouseId())
                .status("DRAFT")
                .note(req.getNote())
                .build();
        pr.setId(UUID.randomUUID().toString());
        pr = prRepository.save(pr);
        saveLines(pr.getId(), req.getLines());
        return toDto(pr);
    }

    @Override
    @Transactional
    public PurchaseRequestDto update(String id, PurchaseRequestSaveRequest req) {
        PurchaseRequest pr = find(id);
        if (!"DRAFT".equals(pr.getStatus())) {
            throw new AppException(CommonErrorCode.CONFLICT, "Chỉ sửa được PR DRAFT");
        }
        pr.setSupplierId(req.getSupplierId());
        pr.setWarehouseId(req.getWarehouseId());
        pr.setNote(req.getNote());
        prRepository.save(pr);
        lineRepository.findByPurchaseRequestIdAndIsDeletedFalse(id).forEach(l -> {
            l.setIsDeleted(true);
            lineRepository.save(l);
        });
        saveLines(id, req.getLines());
        return toDto(pr);
    }

    @Override
    @Transactional
    public void delete(String id) {
        PurchaseRequest pr = find(id);
        if ("PENDING".equals(pr.getStatus())) {
            throw new AppException(CommonErrorCode.CONFLICT, "Không xoá PR đang duyệt");
        }
        pr.setIsDeleted(true);
        prRepository.save(pr);
        lineRepository.findByPurchaseRequestIdAndIsDeletedFalse(id).forEach(l -> {
            if (l.getStockAlertId() != null) {
                alertRepository.findById(l.getStockAlertId()).ifPresent(a -> {
                    if (id.equals(a.getPurchaseRequestId())) {
                        a.setPurchaseRequestId(null);
                        alertRepository.save(a);
                    }
                });
            }
            l.setIsDeleted(true);
            lineRepository.save(l);
        });
    }

    @Override
    @Transactional
    public PurchaseRequestDto submit(String id) {
        PurchaseRequest pr = find(id);
        if (!"DRAFT".equals(pr.getStatus())) {
            throw new AppException(CommonErrorCode.CONFLICT, "Chỉ submit PR DRAFT");
        }
        List<PurchaseRequestLine> lines = lineRepository.findByPurchaseRequestIdAndIsDeletedFalse(id);
        if (lines.isEmpty()) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "PR chưa có dòng");
        }

        // LNK-05 flag OFF → bypass Approval (FE badge via approvalBypassed)
        if (!approvalRequired) {
            pr.setStatus("APPROVED");
            pr.setSubmittedAt(LocalDateTime.now());
            pr.setApprovalRequestId(null);
            PurchaseRequestDto dto = toDto(prRepository.save(pr));
            dto.setApprovalBypassed(true);
            log.info("[PR] submit bypass approval (warehouse.pr.approval.required=false) id={}", id);
            return dto;
        }

        String summary = "PR " + pr.getCode() + " · " + lines.size() + " dòng";

        ApprovalRequest ar = approvalCreator.create(
                SubjectType.PURCHASE_REQUEST.name(),
                pr.getId(),
                summary,
                null,
                null,
                null);
        pr.setApprovalRequestId(ar.getId());
        pr.setStatus("PENDING");
        pr.setSubmittedAt(LocalDateTime.now());
        PurchaseRequestDto dto = toDto(prRepository.save(pr));
        dto.setApprovalBypassed(false);
        return dto;
    }

    private void saveLines(String prId, List<PurchaseRequestSaveRequest.Line> lines) {
        if (lines == null) return;
        for (PurchaseRequestSaveRequest.Line l : lines) {
            PurchaseRequestLine line = PurchaseRequestLine.builder()
                    .purchaseRequestId(prId)
                    .productId(l.getProductId())
                    .warehouseId(l.getWarehouseId())
                    .qty(l.getQty() != null ? l.getQty() : 1.0)
                    .stockAlertId(l.getStockAlertId())
                    .note(l.getNote())
                    .build();
            line.setId(UUID.randomUUID().toString());
            lineRepository.save(line);
            if (l.getStockAlertId() != null) {
                alertRepository.findById(l.getStockAlertId()).ifPresent(a -> {
                    a.setPurchaseRequestId(prId);
                    alertRepository.save(a);
                });
            }
        }
    }

    private double resolveQty(ReorderRule rule, StockAlert alert) {
        if (rule != null && rule.getReorderQty() != null && rule.getReorderQty() > 0) {
            return rule.getReorderQty();
        }
        if (rule != null && rule.getMaxQty() != null && alert.getCurrentQty() != null) {
            double need = rule.getMaxQty() - alert.getCurrentQty();
            if (need > 0) return need;
        }
        if (alert.getMinQty() != null && alert.getCurrentQty() != null) {
            double need = alert.getMinQty() - alert.getCurrentQty();
            if (need > 0) return need;
        }
        return 1.0;
    }

    private String nextCode() {
        return "PR-" + System.currentTimeMillis();
    }

    private PurchaseRequest find(String id) {
        return prRepository.findById(id)
                .filter(p -> Boolean.FALSE.equals(p.getIsDeleted()) || p.getIsDeleted() == null)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "PR không tồn tại"));
    }

    private PurchaseRequestDto toDto(PurchaseRequest pr) {
        List<PurchaseRequestLineDto> lines = lineRepository
                .findByPurchaseRequestIdAndIsDeletedFalse(pr.getId()).stream()
                .map(l -> PurchaseRequestLineDto.builder()
                        .id(l.getId())
                        .productId(l.getProductId())
                        .warehouseId(l.getWarehouseId())
                        .qty(l.getQty())
                        .stockAlertId(l.getStockAlertId())
                        .note(l.getNote())
                        .build())
                .toList();
        return PurchaseRequestDto.builder()
                .id(pr.getId())
                .code(pr.getCode())
                .supplierId(pr.getSupplierId())
                .supplierName(resolveSupplierName(pr.getSupplierId()))
                .warehouseId(pr.getWarehouseId())
                .warehouseName(resolveWarehouseName(pr.getWarehouseId()))
                .status(pr.getStatus())
                .note(pr.getNote())
                .approvalRequestId(pr.getApprovalRequestId())
                .submittedAt(pr.getSubmittedAt() != null ? pr.getSubmittedAt().format(ISO) : null)
                .createdDate(pr.getCreatedDate() != null ? pr.getCreatedDate().format(ISO) : null)
                .lines(lines)
                .build();
    }

    private String resolveSupplierName(String supplierId) {
        if (supplierId == null || supplierId.isBlank()) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT name FROM nccs WHERE id = ? AND COALESCE(is_deleted, false) = false LIMIT 1",
                    String.class, supplierId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private String resolveWarehouseName(String warehouseId) {
        if (warehouseId == null || warehouseId.isBlank()) {
            return null;
        }
        return warehouseRepository.findById(warehouseId).map(Warehouse::getName).orElse(null);
    }
}
