package com.frezo.warehouse.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.response.FePage;
import com.frezo.warehouse.dto.request.PurchaseOrderSaveRequest;
import com.frezo.warehouse.dto.response.PurchaseOrderDto;
import com.frezo.warehouse.dto.response.PurchaseOrderLineDto;
import com.frezo.warehouse.entity.PurchaseOrder;
import com.frezo.warehouse.entity.PurchaseOrderLine;
import com.frezo.warehouse.entity.PurchaseRequest;
import com.frezo.warehouse.entity.PurchaseRequestLine;
import com.frezo.warehouse.entity.StockBalance;
import com.frezo.warehouse.repository.PurchaseOrderLineRepository;
import com.frezo.warehouse.repository.PurchaseOrderRepository;
import com.frezo.warehouse.repository.PurchaseRequestLineRepository;
import com.frezo.warehouse.repository.PurchaseRequestRepository;
import com.frezo.warehouse.repository.StockBalanceRepository;
import com.frezo.warehouse.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderLineRepository poLineRepository;
    private final PurchaseRequestRepository prRepository;
    private final PurchaseRequestLineRepository prLineRepository;
    private final StockBalanceRepository stockBalanceRepository;

    @Override
    public FePage<PurchaseOrderDto> list() {
        return FePage.all(poRepository.findByIsDeletedFalseOrderByCreatedDateDesc().stream()
                .map(this::toDto).toList());
    }

    @Override
    public PurchaseOrderDto get(String id) {
        return toDto(find(id));
    }

    @Override
    @Transactional
    public PurchaseOrderDto create(PurchaseOrderSaveRequest req) {
        PurchaseOrder po = PurchaseOrder.builder()
                .code(nextCode())
                .prId(req.getPrId())
                .supplierId(req.getSupplierId())
                .warehouseId(req.getWarehouseId())
                .status("DRAFT")
                .note(req.getNote())
                .build();
        po.setId(UUID.randomUUID().toString());
        po = poRepository.save(po);
        saveLines(po.getId(), req.getLines());
        return toDto(po);
    }

    @Override
    @Transactional
    public PurchaseOrderDto update(String id, PurchaseOrderSaveRequest req) {
        PurchaseOrder po = find(id);
        if ("RECEIVED".equals(po.getStatus())) {
            throw new AppException(CommonErrorCode.CONFLICT, "Không sửa PO đã nhận hàng");
        }
        po.setSupplierId(req.getSupplierId());
        po.setWarehouseId(req.getWarehouseId());
        po.setNote(req.getNote());
        poRepository.save(po);
        poLineRepository.findByPurchaseOrderIdAndIsDeletedFalse(id).forEach(l -> {
            l.setIsDeleted(true);
            poLineRepository.save(l);
        });
        saveLines(id, req.getLines());
        return toDto(po);
    }

    @Override
    @Transactional
    public void delete(String id) {
        PurchaseOrder po = find(id);
        if ("RECEIVED".equals(po.getStatus())) {
            throw new AppException(CommonErrorCode.CONFLICT, "Không xoá PO đã nhận hàng");
        }
        po.setIsDeleted(true);
        poRepository.save(po);
        poLineRepository.findByPurchaseOrderIdAndIsDeletedFalse(id).forEach(l -> {
            l.setIsDeleted(true);
            poLineRepository.save(l);
        });
    }

    @Override
    @Transactional
    public PurchaseOrderDto createFromPr(String prId) {
        // Idempotent
        var existing = poRepository.findByPrIdAndIsDeletedFalse(prId);
        if (existing.isPresent()) {
            return toDto(existing.get());
        }

        PurchaseRequest pr = prRepository.findById(prId)
                .filter(p -> Boolean.FALSE.equals(p.getIsDeleted()) || p.getIsDeleted() == null)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "PR không tồn tại"));
        if (!"APPROVED".equals(pr.getStatus())) {
            throw new AppException(CommonErrorCode.CONFLICT, "Chỉ tạo PO từ PR APPROVED");
        }

        List<PurchaseRequestLine> prLines = prLineRepository.findByPurchaseRequestIdAndIsDeletedFalse(prId);
        if (prLines.isEmpty()) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "PR không có dòng");
        }

        PurchaseOrder po = PurchaseOrder.builder()
                .code(nextCode())
                .prId(prId)
                .supplierId(pr.getSupplierId())
                .warehouseId(pr.getWarehouseId())
                .status("CONFIRMED")
                .note("Từ PR " + pr.getCode())
                .confirmedAt(LocalDateTime.now())
                .build();
        po.setId(UUID.randomUUID().toString());
        po = poRepository.save(po);

        for (PurchaseRequestLine pl : prLines) {
            PurchaseOrderLine line = PurchaseOrderLine.builder()
                    .purchaseOrderId(po.getId())
                    .prLineId(pl.getId())
                    .productId(pl.getProductId())
                    .warehouseId(pl.getWarehouseId() != null ? pl.getWarehouseId() : pr.getWarehouseId())
                    .qty(pl.getQty())
                    .receivedQty(0.0)
                    .note(pl.getNote())
                    .build();
            line.setId(UUID.randomUUID().toString());
            poLineRepository.save(line);
        }
        return toDto(po);
    }

    @Override
    @Transactional
    public PurchaseOrderDto confirmReceive(String id) {
        PurchaseOrder po = find(id);
        if ("RECEIVED".equals(po.getStatus())) {
            return toDto(po);
        }
        if ("CANCELLED".equals(po.getStatus())) {
            throw new AppException(CommonErrorCode.CONFLICT, "PO đã huỷ");
        }

        List<PurchaseOrderLine> lines = poLineRepository.findByPurchaseOrderIdAndIsDeletedFalse(id);
        for (PurchaseOrderLine line : lines) {
            double qty = line.getQty() != null ? line.getQty() : 0.0;
            line.setReceivedQty(qty);
            poLineRepository.save(line);
            // Stub tăng tồn kho
            increaseStock(line.getProductId(),
                    line.getWarehouseId() != null ? line.getWarehouseId() : po.getWarehouseId(),
                    qty);
        }
        po.setStatus("RECEIVED");
        po.setReceivedAt(LocalDateTime.now());
        log.info("[PO] confirm-receive {} — {} lines, stock increased", id, lines.size());
        return toDto(poRepository.save(po));
    }

    private void increaseStock(String productId, String warehouseId, double qty) {
        if (productId == null || warehouseId == null || qty <= 0) return;
        StockBalance balance = stockBalanceRepository
                .findByProductIdAndWarehouseIdAndLocationIdAndBatchId(productId, warehouseId, null, null)
                .orElseGet(() -> {
                    StockBalance b = StockBalance.builder()
                            .productId(productId)
                            .warehouseId(warehouseId)
                            .locationId(null)
                            .batchId(null)
                            .quantityOnHand(0.0)
                            .quantityReserved(0.0)
                            .quantityAvailable(0.0)
                            .build();
                    b.setId(UUID.randomUUID().toString());
                    return stockBalanceRepository.save(b);
                });
        double onHand = balance.getQuantityOnHand() != null ? balance.getQuantityOnHand() : 0.0;
        double reserved = balance.getQuantityReserved() != null ? balance.getQuantityReserved() : 0.0;
        balance.setQuantityOnHand(onHand + qty);
        balance.setQuantityAvailable(balance.getQuantityOnHand() - reserved);
        stockBalanceRepository.save(balance);
    }

    private void saveLines(String poId, List<PurchaseOrderSaveRequest.Line> lines) {
        if (lines == null) return;
        for (PurchaseOrderSaveRequest.Line l : lines) {
            PurchaseOrderLine line = PurchaseOrderLine.builder()
                    .purchaseOrderId(poId)
                    .prLineId(l.getPrLineId())
                    .productId(l.getProductId())
                    .warehouseId(l.getWarehouseId())
                    .qty(l.getQty() != null ? l.getQty() : 1.0)
                    .receivedQty(0.0)
                    .note(l.getNote())
                    .build();
            line.setId(UUID.randomUUID().toString());
            poLineRepository.save(line);
        }
    }

    private String nextCode() {
        return "PO-" + System.currentTimeMillis();
    }

    private PurchaseOrder find(String id) {
        return poRepository.findById(id)
                .filter(p -> Boolean.FALSE.equals(p.getIsDeleted()) || p.getIsDeleted() == null)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "PO không tồn tại"));
    }

    private PurchaseOrderDto toDto(PurchaseOrder po) {
        List<PurchaseOrderLineDto> lines = poLineRepository
                .findByPurchaseOrderIdAndIsDeletedFalse(po.getId()).stream()
                .map(l -> PurchaseOrderLineDto.builder()
                        .id(l.getId())
                        .prLineId(l.getPrLineId())
                        .productId(l.getProductId())
                        .warehouseId(l.getWarehouseId())
                        .qty(l.getQty())
                        .receivedQty(l.getReceivedQty())
                        .note(l.getNote())
                        .build())
                .toList();
        return PurchaseOrderDto.builder()
                .id(po.getId())
                .code(po.getCode())
                .prId(po.getPrId())
                .supplierId(po.getSupplierId())
                .warehouseId(po.getWarehouseId())
                .status(po.getStatus())
                .note(po.getNote())
                .confirmedAt(po.getConfirmedAt() != null ? po.getConfirmedAt().format(ISO) : null)
                .receivedAt(po.getReceivedAt() != null ? po.getReceivedAt().format(ISO) : null)
                .createdDate(po.getCreatedDate() != null ? po.getCreatedDate().format(ISO) : null)
                .lines(lines)
                .build();
    }
}
