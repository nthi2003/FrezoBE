package com.frezo.warehouse.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.utils.SecureCodeGenerator;
import com.frezo.product.entity.Product;
import com.frezo.product.repository.ProductRepository;
import com.frezo.warehouse.common.WarehouseErrorCode;
import com.frezo.warehouse.dto.request.ShrinkageCreateRequest;
import com.frezo.warehouse.dto.response.ShrinkageResponse;
import com.frezo.warehouse.entity.*;
import com.frezo.warehouse.repository.*;
import com.frezo.warehouse.service.StockShrinkageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockShrinkageServiceImpl implements StockShrinkageService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final StockShrinkageRepository shrinkageRepository;
    private final StockShrinkageLineRepository lineRepository;
    private final StockBatchRepository batchRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ShrinkageResponse create(ShrinkageCreateRequest request) {
        StockShrinkage shrinkage = StockShrinkage.builder()
                .shrinkageCode(SecureCodeGenerator.generateCode("SHR"))
                .warehouseId(request.getWarehouseId())
                .status("DRAFT")
                .note(request.getNote())
                .build();
        shrinkage = shrinkageRepository.save(shrinkage);

        if (request.getLines() != null) {
            for (ShrinkageCreateRequest.ShrinkageLineRequest lineReq : request.getLines()) {
                StockShrinkageLine line = StockShrinkageLine.builder()
                        .shrinkageId(shrinkage.getId())
                        .batchId(lineReq.getBatchId())
                        .productId(lineReq.getProductId())
                        .reason(lineReq.getReason().toUpperCase())
                        .qty(lineReq.getQty())
                        .note(lineReq.getNote())
                        .build();
                lineRepository.save(line);
            }
        }
        return buildResponse(shrinkage);
    }

    @Override
    @Transactional
    public ShrinkageResponse confirm(String id) {
        StockShrinkage shrinkage = shrinkageRepository.findById(id)
                .filter(s -> Boolean.FALSE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new AppException(WarehouseErrorCode.SHRINKAGE_NOT_FOUND));
        if (!"DRAFT".equals(shrinkage.getStatus())) {
            throw new AppException(WarehouseErrorCode.SHRINKAGE_ALREADY_CONFIRMED);
        }

        List<StockShrinkageLine> lines = lineRepository.findByShrinkageIdAndIsDeletedFalse(id);
        List<StockLedger> ledgerEntries = new ArrayList<>();

        for (StockShrinkageLine line : lines) {
            StockBatch batch = batchRepository.findById(line.getBatchId())
                    .orElseThrow(() -> new AppException(WarehouseErrorCode.BATCH_NOT_FOUND));
            double onHand = batch.getQtyOnHand() != null ? batch.getQtyOnHand() : 0;
            if (onHand < line.getQty()) {
                throw new AppException(WarehouseErrorCode.BATCH_INSUFFICIENT);
            }

            batch.setQtyOnHand(onHand - line.getQty());
            if (batch.getQtyOnHand() <= 0) {
                batch.setStatus("DEPLETED");
            }
            batchRepository.save(batch);

            deductStockBalance(line.getProductId(), shrinkage.getWarehouseId(),
                    batch.getWarehouseLocationId(), batch.getId(), line.getQty());

            StockLedger ledger = StockLedger.builder()
                    .productId(line.getProductId())
                    .batchId(batch.getId())
                    .locationId(batch.getWarehouseLocationId())
                    .warehouseId(shrinkage.getWarehouseId())
                    .transactionType("OUT")
                    .quantity(-line.getQty())
                    .referenceType("SHRINKAGE")
                    .referenceId(shrinkage.getId())
                    .note("Hao hụt " + line.getReason() + ": " + (line.getNote() != null ? line.getNote() : ""))
                    .build();
            ledgerEntries.add(ledger);
        }

        stockLedgerRepository.saveAll(ledgerEntries);
        shrinkage.setStatus("CONFIRMED");
        shrinkage.setConfirmedAt(LocalDateTime.now());
        shrinkageRepository.save(shrinkage);
        return buildResponse(shrinkage);
    }

    @Override
    @Transactional
    public void cancel(String id, String reason) {
        StockShrinkage shrinkage = shrinkageRepository.findById(id)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.SHRINKAGE_NOT_FOUND));
        if ("CONFIRMED".equals(shrinkage.getStatus())) {
            throw new AppException(WarehouseErrorCode.SHRINKAGE_CANNOT_CANCEL_CONFIRMED);
        }
        shrinkage.setStatus("CANCELLED");
        shrinkage.setNote(reason);
        shrinkageRepository.save(shrinkage);
    }

    @Override
    public ShrinkageResponse getById(String id) {
        StockShrinkage shrinkage = shrinkageRepository.findById(id)
                .filter(s -> Boolean.FALSE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new AppException(WarehouseErrorCode.SHRINKAGE_NOT_FOUND));
        return buildResponse(shrinkage);
    }

    @Override
    public com.frezo.common.response.FePage<ShrinkageResponse> list(String warehouseId, String status) {
        List<StockShrinkage> list;
        if (warehouseId != null && !warehouseId.isBlank()) {
            list = shrinkageRepository.findByWarehouseIdAndIsDeletedFalseOrderByCreatedDateDesc(warehouseId);
        } else if (status != null && !status.isBlank()) {
            list = shrinkageRepository.findByStatusAndIsDeletedFalseOrderByCreatedDateDesc(status.toUpperCase());
        } else {
            list = shrinkageRepository.findByIsDeletedFalseOrderByCreatedDateDesc();
        }
        if (status != null && !status.isBlank() && warehouseId != null && !warehouseId.isBlank()) {
            String st = status.toUpperCase();
            list = list.stream().filter(s -> st.equals(s.getStatus())).toList();
        }
        return com.frezo.common.response.FePage.all(list.stream().map(this::buildResponse).toList());
    }

    private ShrinkageResponse buildResponse(StockShrinkage shrinkage) {
        Warehouse wh = warehouseRepository.findById(shrinkage.getWarehouseId()).orElse(null);
        List<StockShrinkageLine> lines = lineRepository.findByShrinkageIdAndIsDeletedFalse(shrinkage.getId());
        List<ShrinkageResponse.ShrinkageLineResponse> lineDtos = lines.stream().map(l -> {
            Product p = productRepository.findById(l.getProductId()).orElse(null);
            StockBatch batch = batchRepository.findById(l.getBatchId()).orElse(null);
            return ShrinkageResponse.ShrinkageLineResponse.builder()
                    .id(l.getId())
                    .batchId(l.getBatchId())
                    .batchCode(batch != null ? batch.getBatchCode() : null)
                    .productId(l.getProductId())
                    .productCode(p != null ? p.getCode() : null)
                    .productName(p != null ? p.getName() : null)
                    .reason(l.getReason())
                    .qty(l.getQty())
                    .note(l.getNote())
                    .expiryDate(batch != null && batch.getExpiryDate() != null
                            ? batch.getExpiryDate().format(DATE_FMT) : null)
                    .build();
        }).toList();

        return ShrinkageResponse.builder()
                .id(shrinkage.getId())
                .shrinkageCode(shrinkage.getShrinkageCode())
                .warehouseId(shrinkage.getWarehouseId())
                .warehouseName(wh != null ? wh.getName() : null)
                .status(shrinkage.getStatus())
                .note(shrinkage.getNote())
                .confirmedAt(shrinkage.getConfirmedAt() != null ? shrinkage.getConfirmedAt().format(ISO) : null)
                .createdAt(shrinkage.getCreatedDate() != null ? shrinkage.getCreatedDate().format(ISO) : null)
                .lines(lineDtos)
                .build();
    }

    private void deductStockBalance(String productId, String warehouseId, String locationId,
                                    String batchId, double qty) {
        StockBalance balance = stockBalanceRepository
                .findByProductIdAndWarehouseIdAndLocationIdAndBatchId(
                        productId, warehouseId, locationId, batchId)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.STOCK_BALANCE_NOT_FOUND));
        if (balance.getQuantityOnHand() < qty) {
            throw new AppException(WarehouseErrorCode.STOCK_BALANCE_INSUFFICIENT);
        }
        balance.setQuantityOnHand(balance.getQuantityOnHand() - qty);
        balance.setQuantityAvailable(balance.getQuantityOnHand() - balance.getQuantityReserved());
        stockBalanceRepository.save(balance);
    }
}
