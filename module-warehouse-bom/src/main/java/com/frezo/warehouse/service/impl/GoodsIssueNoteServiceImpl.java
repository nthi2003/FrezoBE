package com.frezo.warehouse.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.warehouse.common.WarehouseErrorCode;
import com.frezo.common.response.PageResponse;
import com.frezo.common.utils.SecureCodeGenerator;
import com.frezo.warehouse.dto.request.GinConfirmRequest;
import com.frezo.warehouse.dto.request.GinCreateRequest;
import com.frezo.warehouse.dto.response.GinResponse;
import com.frezo.warehouse.entity.*;
import com.frezo.warehouse.mapper.GoodsIssueNoteMapper;
import com.frezo.warehouse.repository.*;
import com.frezo.warehouse.service.GoodsIssueNoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoodsIssueNoteServiceImpl implements GoodsIssueNoteService {

    private final GoodsIssueNoteRepository ginRepository;
    private final GoodsIssueNoteItemRepository ginItemRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final GoodsIssueNoteMapper ginMapper;
    private final StockBatchRepository stockBatchRepository;
    private final WarehouseRepository warehouseRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public GinResponse getById(String id) {
        GoodsIssueNote gin = ginRepository.findById(id)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.GIN_NOT_FOUND));
        return buildResponse(gin);
    }

    @Override
    public GinResponse getByCode(String ginCode) {
        GoodsIssueNote gin = ginRepository.findByGinCode(ginCode)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.GIN_NOT_FOUND));
        return buildResponse(gin);
    }

    @Override
    public PageResponse<GinResponse> filter(String status, String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Specification<GoodsIssueNote> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("ginCode")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("documentNo"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("note"), "")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        Page<GoodsIssueNote> ginPage = ginRepository.findAll(spec, pageable);
        List<GinResponse> responses = ginPage.getContent().stream().map(this::buildResponse).toList();
        return PageResponse.of(page, size, ginPage, responses);
    }

    @Override
    @Transactional
    public GinResponse create(GinCreateRequest request) {
        GoodsIssueNote gin = ginMapper.toEntity(request);
        gin.setGinCode(SecureCodeGenerator.generateCode("GIN"));
        gin = ginRepository.save(gin);

        if (request.getItems() != null) {
            double totalValue = 0;
            for (GinCreateRequest.GinItemRequest itemReq : request.getItems()) {
                GoodsIssueNoteItem item = ginMapper.toItemEntity(itemReq);
                item.setGinId(gin.getId());
                if (item.getQtyIssued() == null) {
                    item.setQtyIssued(0.0);
                }
                ginItemRepository.save(item);

                if (item.getQtyRequested() != null && item.getUnitCost() != null) {
                    totalValue += item.getQtyRequested() * item.getUnitCost();
                }
            }
            gin.setTotalValue(totalValue);
            ginRepository.save(gin);
        }

        return buildResponse(gin);
    }

    @Override
    @Transactional
    public GinResponse submit(String id) {
        GoodsIssueNote gin = ginRepository.findById(id)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.GIN_NOT_FOUND));
        if (!"DRAFT".equals(gin.getStatus())) {
            throw new AppException(WarehouseErrorCode.GIN_INVALID_STATUS);
        }
        gin.setStatus("PENDING_APPROVAL");
        ginRepository.save(gin);
        return buildResponse(gin);
    }

    @Override
    @Transactional
    public GinResponse approve(String id) {
        GoodsIssueNote gin = ginRepository.findById(id)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.GIN_NOT_FOUND));
        String st = gin.getStatus();
        if (!"PENDING_APPROVAL".equals(st) && !"DRAFT".equals(st)) {
            throw new AppException(WarehouseErrorCode.GIN_INVALID_STATUS);
        }
        gin.setStatus("APPROVED");
        gin.setApprovedBy(gin.getCreatedBy());
        gin.setApprovedAt(LocalDateTime.now());
        ginRepository.save(gin);
        return buildResponse(gin);
    }

    @Override
    @Transactional
    public GinResponse confirm(String id, GinConfirmRequest request) {
        GoodsIssueNote gin = ginRepository.findById(id)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.GIN_NOT_FOUND));

        String st = gin.getStatus();
        if (!"DRAFT".equals(st) && !"APPROVED".equals(st)) {
            throw new AppException(WarehouseErrorCode.GIN_INVALID_STATUS);
        }

        List<GoodsIssueNoteItem> items = ginItemRepository.findByGinId(id);
        List<StockLedger> ledgerEntries = new ArrayList<>();
        double totalValue = 0;

        for (GoodsIssueNoteItem item : items) {
            GinConfirmRequest.GinConfirmItem confirmItem = findConfirmItem(request, item.getId());
            double qtyIssued = confirmItem != null ? confirmItem.getQtyIssued() : item.getQtyIssued();

            if (confirmItem != null && confirmItem.getBatchId() != null) {
                item.setBatchId(confirmItem.getBatchId());
            }
            if (confirmItem != null && confirmItem.getLocationId() != null) {
                item.setLocationId(confirmItem.getLocationId());
            }
            item.setQtyIssued(qtyIssued);
            ginItemRepository.save(item);

            if (qtyIssued > 0) {
                deductBatchQty(item.getBatchId(), qtyIssued);

                StockLedger ledger = StockLedger.builder()
                        .productId(item.getProductId())
                        .batchId(item.getBatchId())
                        .locationId(item.getLocationId())
                        .transactionType("OUT")
                        .quantity(-qtyIssued)
                        .unitCost(item.getUnitCost())
                        .totalValue(-qtyIssued * (item.getUnitCost() != null ? item.getUnitCost() : 0))
                        .referenceType("GIN")
                        .referenceId(gin.getId())
                        .note("Xuất kho từ phiếu " + gin.getGinCode())
                        .build();
                ledgerEntries.add(ledger);
                totalValue += qtyIssued * (item.getUnitCost() != null ? item.getUnitCost() : 0);

                deductStockBalance(item.getProductId(), gin.getWarehouseId(),
                        item.getLocationId(), item.getBatchId(), qtyIssued);
            }
        }

        stockLedgerRepository.saveAll(ledgerEntries);

        StockTransaction transaction = StockTransaction.builder()
                .transactionCode(SecureCodeGenerator.generateCode("ST"))
                .transactionType("ISSUE")
                .referenceType("GIN")
                .referenceId(gin.getId())
                .warehouseId(gin.getWarehouseId())
                .totalAmount(totalValue)
                .status("CONFIRMED")
                .note("Từ phiếu xuất kho " + gin.getGinCode())
                .build();
        stockTransactionRepository.save(transaction);

        gin.setStatus("CONFIRMED");
        gin.setTotalValue(totalValue);
        gin.setIssuedBy(gin.getCreatedBy());
        gin.setIssuedAt(LocalDateTime.now());
        ginRepository.save(gin);

        return buildResponse(gin);
    }

    @Override
    @Transactional
    public void batchConfirm(List<String> ids) {
        for (String id : ids) {
            GoodsIssueNote gin = ginRepository.findById(id)
                    .orElseThrow(() -> new AppException(WarehouseErrorCode.GIN_NOT_FOUND));
            String st = gin.getStatus();
            if (!"DRAFT".equals(st) && !"APPROVED".equals(st)) {
                throw new AppException(WarehouseErrorCode.GIN_INVALID_STATUS);
            }
        }
        for (String id : ids) {
            confirm(id, null);
        }
    }

    @Override
    @Transactional
    public void cancel(String id, String reason) {
        GoodsIssueNote gin = ginRepository.findById(id)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.GIN_NOT_FOUND));

        if ("CONFIRMED".equals(gin.getStatus())) {
            throw new AppException(WarehouseErrorCode.GIN_CANNOT_CANCEL_CONFIRMED);
        }

        gin.setStatus("CANCELLED");
        gin.setNote(reason);
        ginRepository.save(gin);
    }

    @Override
    @Transactional
    public void batchCancel(List<String> ids, String reason) {
        for (String id : ids) {
            cancel(id, reason);
        }
    }

    @Override
    @Transactional
    public void delete(String id) {
        GoodsIssueNote gin = ginRepository.findById(id)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.GIN_NOT_FOUND));
        if ("CONFIRMED".equals(gin.getStatus())) {
            throw new AppException(WarehouseErrorCode.GIN_CANNOT_DELETE_CONFIRMED);
        }
        ginItemRepository.findByGinId(id).forEach(ginItemRepository::delete);
        ginRepository.delete(gin);
    }

    private GinResponse buildResponse(GoodsIssueNote gin) {
        GinResponse response = ginMapper.toResponse(gin);
        if (gin.getWarehouseId() != null) {
            warehouseRepository.findById(gin.getWarehouseId()).ifPresent(wh -> {
                response.setWarehouseName(wh.getName());
                response.setWarehouseCode(wh.getCode());
            });
        }
        if (gin.getTransferWarehouseId() != null) {
            warehouseRepository.findById(gin.getTransferWarehouseId()).ifPresent(wh ->
                    response.setTransferWarehouseName(wh.getName()));
        }
        response.setCustomerName(resolveCustomerName(gin.getCustomerId()));
        List<GoodsIssueNoteItem> items = ginItemRepository.findByGinId(gin.getId());
        response.setItems(items.stream().map(ginMapper::toItemResponse).toList());
        return response;
    }

    private String resolveCustomerName(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT name FROM customers WHERE id = ? AND COALESCE(is_deleted, false) = false LIMIT 1",
                    String.class, customerId);
        } catch (EmptyResultDataAccessException ex) {
            try {
                return jdbcTemplate.queryForObject(
                        "SELECT name FROM khs WHERE id = ? AND COALESCE(is_deleted, false) = false LIMIT 1",
                        String.class, customerId);
            } catch (EmptyResultDataAccessException ex2) {
                return null;
            }
        }
    }

    private GinConfirmRequest.GinConfirmItem findConfirmItem(GinConfirmRequest request, String itemId) {
        if (request.getItems() == null) return null;
        return request.getItems().stream()
                .filter(i -> i.getItemId().equals(itemId))
                .findFirst().orElse(null);
    }

    private void deductStockBalance(String productId, String warehouseId, String locationId, String batchId, double qty) {
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

    private void deductBatchQty(String batchId, double qty) {
        if (batchId == null || batchId.isBlank()) return;
        StockBatch batch = stockBatchRepository.findById(batchId)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.BATCH_NOT_FOUND));
        double onHand = batch.getQtyOnHand() != null ? batch.getQtyOnHand() : 0;
        if (onHand < qty) {
            throw new AppException(WarehouseErrorCode.BATCH_INSUFFICIENT);
        }
        batch.setQtyOnHand(onHand - qty);
        if (batch.getQtyOnHand() <= 0) {
            batch.setStatus("DEPLETED");
        }
        stockBatchRepository.save(batch);
    }
}
