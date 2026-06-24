package com.frezo.warehouse.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.response.PageResponse;
import com.frezo.common.utils.SecureCodeGenerator;
import com.frezo.warehouse.dto.request.StockAdjustmentCreateRequest;
import com.frezo.warehouse.dto.response.StockAdjustmentResponse;
import com.frezo.warehouse.entity.*;
import com.frezo.warehouse.repository.*;
import com.frezo.warehouse.service.StockAdjustmentService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockAdjustmentServiceImpl implements StockAdjustmentService {

    private final StockAdjustmentRepository adjustmentRepository;
    private final StockAdjustmentItemRepository adjustmentItemRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockLedgerRepository stockLedgerRepository;

    @Override
    @Transactional
    public StockAdjustmentResponse create(StockAdjustmentCreateRequest request) {
        StockAdjustment adj = StockAdjustment.builder()
                .adjustmentCode(SecureCodeGenerator.generateCode("ADJ"))
                .warehouseId(request.getWarehouseId())
                .status("DRAFT")
                .note(request.getNote())
                .build();
        adj = adjustmentRepository.save(adj);

        double totalDiffValue = 0;
        if (request.getItems() != null) {
            for (StockAdjustmentCreateRequest.AdjustmentItemRequest itemReq : request.getItems()) {
                double expected = itemReq.getExpectedQty() != null ? itemReq.getExpectedQty() : 0;
                double actual = itemReq.getActualQty() != null ? itemReq.getActualQty() : 0;
                double diff = actual - expected;
                double cost = itemReq.getUnitCost() != null ? itemReq.getUnitCost() : 0;

                StockAdjustmentItem item = StockAdjustmentItem.builder()
                        .adjustmentId(adj.getId())
                        .productId(itemReq.getProductId())
                        .batchId(itemReq.getBatchId())
                        .locationId(itemReq.getLocationId())
                        .expectedQty(expected)
                        .actualQty(actual)
                        .diffQty(diff)
                        .unitCost(cost)
                        .totalDiffValue(diff * cost)
                        .reason(itemReq.getReason())
                        .build();
                adjustmentItemRepository.save(item);
                totalDiffValue += diff * cost;
            }
        }

        adj.setTotalDiffValue(totalDiffValue);
        adjustmentRepository.save(adj);

        return buildResponse(adj);
    }

    @Override
    @Transactional
    public StockAdjustmentResponse confirm(String id) {
        StockAdjustment adj = adjustmentRepository.findById(id)
                .orElseThrow(() -> new QTHTException("stock.adjustment.not.found", HttpStatus.NOT_FOUND));

        if (!"DRAFT".equals(adj.getStatus())) {
            throw new QTHTException("stock.adjustment.already.confirmed", HttpStatus.BAD_REQUEST);
        }

        List<StockAdjustmentItem> items = adjustmentItemRepository.findByAdjustmentId(id);
        List<StockLedger> ledgerEntries = new ArrayList<>();
        double totalDiffValue = 0;

        for (StockAdjustmentItem item : items) {
            if (item.getDiffQty() == null || item.getDiffQty() == 0) continue;

            String transactionType = item.getDiffQty() > 0 ? "IN" : "OUT";
            double absQty = Math.abs(item.getDiffQty());

            StockLedger ledger = StockLedger.builder()
                    .productId(item.getProductId())
                    .batchId(item.getBatchId())
                    .locationId(item.getLocationId())
                    .warehouseId(adj.getWarehouseId())
                    .transactionType(transactionType)
                    .quantity(absQty)
                    .unitCost(item.getUnitCost())
                    .totalValue(absQty * (item.getUnitCost() != null ? item.getUnitCost() : 0))
                    .referenceType("ADJ")
                    .referenceId(adj.getId())
                    .note("Điều chỉnh tồn kho: " + (item.getReason() != null ? item.getReason() : ""))
                    .build();
            ledgerEntries.add(ledger);

            updateStockBalance(item.getProductId(), adj.getWarehouseId(),
                    item.getLocationId(), item.getBatchId(), item.getDiffQty());

            totalDiffValue += item.getTotalDiffValue() != null ? item.getTotalDiffValue() : 0;
        }

        stockLedgerRepository.saveAll(ledgerEntries);

        adj.setStatus("CONFIRMED");
        adj.setTotalDiffValue(totalDiffValue);
        adj.setAdjustedAt(LocalDateTime.now());
        adj.setAdjustedBy(adj.getCreatedBy());
        adjustmentRepository.save(adj);

        return buildResponse(adj);
    }

    @Override
    @Transactional
    public void cancel(String id, String reason) {
        StockAdjustment adj = adjustmentRepository.findById(id)
                .orElseThrow(() -> new QTHTException("stock.adjustment.not.found", HttpStatus.NOT_FOUND));
        if ("CONFIRMED".equals(adj.getStatus())) {
            throw new QTHTException("stock.adjustment.cannot.cancel.confirmed", HttpStatus.BAD_REQUEST);
        }
        adj.setStatus("CANCELLED");
        adj.setNote(reason);
        adjustmentRepository.save(adj);
    }

    @Override
    public StockAdjustmentResponse getById(String id) {
        StockAdjustment adj = adjustmentRepository.findById(id)
                .orElseThrow(() -> new QTHTException("stock.adjustment.not.found", HttpStatus.NOT_FOUND));
        return buildResponse(adj);
    }

    @Override
    public StockAdjustmentResponse getByCode(String code) {
        StockAdjustment adj = adjustmentRepository.findByAdjustmentCode(code)
                .orElseThrow(() -> new QTHTException("stock.adjustment.not.found", HttpStatus.NOT_FOUND));
        return buildResponse(adj);
    }

    @Override
    public PageResponse<StockAdjustmentResponse> filter(String status, String warehouseId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<StockAdjustment> adjPage = adjustmentRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (warehouseId != null && !warehouseId.isBlank()) {
                predicates.add(cb.equal(root.get("warehouseId"), warehouseId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        List<StockAdjustmentResponse> responses = adjPage.getContent().stream()
                .map(this::buildResponse).toList();
        return PageResponse.of(page, size, adjPage, responses);
    }

    @Override
    @Transactional
    public void delete(String id) {
        StockAdjustment adj = adjustmentRepository.findById(id)
                .orElseThrow(() -> new QTHTException("stock.adjustment.not.found", HttpStatus.NOT_FOUND));
        if ("CONFIRMED".equals(adj.getStatus())) {
            throw new QTHTException("stock.adjustment.cannot.delete.confirmed", HttpStatus.BAD_REQUEST);
        }
        adjustmentItemRepository.deleteByAdjustmentId(id);
        adjustmentRepository.delete(adj);
    }

    private StockAdjustmentResponse buildResponse(StockAdjustment adj) {
        StockAdjustmentResponse r = new StockAdjustmentResponse();
        r.setId(adj.getId());
        r.setAdjustmentCode(adj.getAdjustmentCode());
        r.setWarehouseId(adj.getWarehouseId());
        r.setStatus(adj.getStatus());
        r.setTotalDiffValue(adj.getTotalDiffValue());
        r.setAdjustedBy(adj.getAdjustedBy());
        r.setAdjustedAt(adj.getAdjustedAt());
        r.setNote(adj.getNote());
        r.setCreatedDate(adj.getCreatedDate());

        List<StockAdjustmentItem> items = adjustmentItemRepository.findByAdjustmentId(adj.getId());
        r.setItems(items.stream().map(item -> {
            StockAdjustmentResponse.AdjustmentItemResponse ir = new StockAdjustmentResponse.AdjustmentItemResponse();
            ir.setId(item.getId());
            ir.setAdjustmentId(item.getAdjustmentId());
            ir.setProductId(item.getProductId());
            ir.setBatchId(item.getBatchId());
            ir.setLocationId(item.getLocationId());
            ir.setExpectedQty(item.getExpectedQty());
            ir.setActualQty(item.getActualQty());
            ir.setDiffQty(item.getDiffQty());
            ir.setUnitCost(item.getUnitCost());
            ir.setTotalDiffValue(item.getTotalDiffValue());
            ir.setReason(item.getReason());
            return ir;
        }).toList());

        return r;
    }

    private void updateStockBalance(String productId, String warehouseId, String locationId, String batchId, double diffQty) {
        StockBalance balance = stockBalanceRepository
                .findByProductIdAndWarehouseIdAndLocationIdAndBatchId(
                        productId, warehouseId, locationId, batchId)
                .orElseGet(() -> {
                    StockBalance newBal = StockBalance.builder()
                            .productId(productId)
                            .warehouseId(warehouseId)
                            .locationId(locationId)
                            .batchId(batchId)
                            .quantityOnHand(0.0)
                            .quantityReserved(0.0)
                            .quantityAvailable(0.0)
                            .build();
                    return stockBalanceRepository.save(newBal);
                });

        balance.setQuantityOnHand(balance.getQuantityOnHand() + diffQty);
        balance.setQuantityAvailable(balance.getQuantityOnHand() - balance.getQuantityReserved());
        stockBalanceRepository.save(balance);
    }
}
