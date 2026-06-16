package com.frezo.warehouse.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.QTHTException;
import com.frezo.common.response.PageResponse;
import com.frezo.common.utils.SecureCodeGenerator;
import com.frezo.product.entity.Batch;
import com.frezo.product.repository.BatchRepository;
import com.frezo.warehouse.dto.request.GrnConfirmRequest;
import com.frezo.warehouse.dto.request.GrnCreateRequest;
import com.frezo.warehouse.dto.response.GrnResponse;
import com.frezo.warehouse.entity.*;
import com.frezo.warehouse.mapper.GoodsReceiptNoteMapper;
import com.frezo.warehouse.repository.*;
import com.frezo.warehouse.service.GoodsReceiptNoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoodsReceiptNoteServiceImpl implements GoodsReceiptNoteService {

    private final GoodsReceiptNoteRepository grnRepository;
    private final GoodsReceiptNoteItemRepository grnItemRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final GoodsReceiptNoteMapper grnMapper;
    private final BatchRepository batchRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public GrnResponse getById(String id) {
        GoodsReceiptNote grn = grnRepository.findById(id)
                .orElseThrow(() -> new QTHTException("goods.receipt.note.not.found", HttpStatus.NOT_FOUND));
        return buildResponse(grn);
    }

    @Override
    public GrnResponse getByCode(String grnCode) {
        GoodsReceiptNote grn = grnRepository.findByGrnCode(grnCode)
                .orElseThrow(() -> new QTHTException("goods.receipt.note.not.found", HttpStatus.NOT_FOUND));
        return buildResponse(grn);
    }

    @Override
    public PageResponse<GrnResponse> filter(String status, String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<GoodsReceiptNote> grnPage;
        if (status != null && !status.isBlank()) {
            grnPage = grnRepository.findAll((root, query, cb) ->
                    cb.equal(root.get("status"), status), pageable);
        } else {
            grnPage = grnRepository.findAll(pageable);
        }
        List<GrnResponse> responses = grnPage.getContent().stream()
                .map(this::buildResponse).toList();
        return PageResponse.of(page, size, grnPage, responses);
    }

    @Override
    @Transactional
    public GrnResponse create(GrnCreateRequest request) {
        GoodsReceiptNote grn = grnMapper.toEntity(request);
        grn.setGrnCode(SecureCodeGenerator.generateCode("GRN"));
        grn = grnRepository.save(grn);

        if (request.getItems() != null) {
            double totalValue = 0;
            for (GrnCreateRequest.GrnItemRequest itemReq : request.getItems()) {
                GoodsReceiptNoteItem item = grnMapper.toItemEntity(itemReq);
                item.setGrnId(grn.getId());
                if (item.getQtyReceived() == null) {
                    item.setQtyReceived(0.0);
                }
                grnItemRepository.save(item);

                if (item.getQtyExpected() != null && item.getUnitCost() != null) {
                    totalValue += item.getQtyExpected() * item.getUnitCost();
                }
            }
            grn.setTotalValue(totalValue);
            grnRepository.save(grn);
        }

        return buildResponse(grn);
    }

    @Override
    @Transactional
    public GrnResponse confirm(String id, GrnConfirmRequest request) {
        GoodsReceiptNote grn = grnRepository.findById(id)
                .orElseThrow(() -> new QTHTException("goods.receipt.note.not.found", HttpStatus.NOT_FOUND));

        if (!"DRAFT".equals(grn.getStatus())) {
            throw new QTHTException("goods.receipt.note.already.confirmed", HttpStatus.BAD_REQUEST);
        }

        List<GoodsReceiptNoteItem> items = grnItemRepository.findByGrnId(id);
        List<StockLedger> ledgerEntries = new ArrayList<>();
        double totalValue = 0;

        for (GoodsReceiptNoteItem item : items) {
            GrnConfirmRequest.GrnConfirmItem confirmItem = findConfirmItem(request, item.getId());
            double qtyReceived = confirmItem != null ? confirmItem.getQtyReceived() : item.getQtyReceived();

            item.setQtyReceived(qtyReceived);
            grnItemRepository.save(item);

            if (qtyReceived > 0) {
                if (item.getBatchId() == null && confirmItem != null && confirmItem.getBatchCode() != null) {
                    Batch newBatch = Batch.builder()
                            .productId(item.getProductId())
                            .batchCode(confirmItem.getBatchCode())
                            .initialQuantity(qtyReceived)
                            .currentQuantity(qtyReceived)
                            .importDate(LocalDate.now())
                            .costPrice(item.getUnitCost())
                            .build();
                    newBatch = batchRepository.save(newBatch);
                    item.setBatchId(newBatch.getId());
                    grnItemRepository.save(item);
                }
                StockLedger ledger = StockLedger.builder()
                        .productId(item.getProductId())
                        .batchId(item.getBatchId())
                        .locationId(item.getLocationId())
                        .transactionType("IN")
                        .quantity(qtyReceived)
                        .unitCost(item.getUnitCost())
                        .totalValue(qtyReceived * (item.getUnitCost() != null ? item.getUnitCost() : 0))
                        .referenceType("GRN")
                        .referenceId(grn.getId())
                        .note("Nhập kho từ phiếu " + grn.getGrnCode())
                        .build();
                ledgerEntries.add(ledger);
                totalValue += qtyReceived * (item.getUnitCost() != null ? item.getUnitCost() : 0);

                updateStockBalance(item.getProductId(), grn.getWarehouseId(),
                        item.getLocationId(), item.getBatchId(), qtyReceived);
            }
        }

        stockLedgerRepository.saveAll(ledgerEntries);

        StockTransaction transaction = StockTransaction.builder()
                .transactionCode(SecureCodeGenerator.generateCode("ST"))
                .transactionType("RECEIPT")
                .referenceType("GRN")
                .referenceId(grn.getId())
                .warehouseId(grn.getWarehouseId())
                .totalAmount(totalValue)
                .status("CONFIRMED")
                .note("Từ phiếu nhập kho " + grn.getGrnCode())
                .build();
        stockTransactionRepository.save(transaction);

        grn.setStatus("CONFIRMED");
        grn.setTotalValue(totalValue);
        grn.setReceivedBy(grn.getCreatedBy());
        grn.setReceivedAt(LocalDateTime.now());
        grnRepository.save(grn);

        eventPublisher.publishEvent(new GrnConfirmedEvent(this, grn.getId(), grn.getGrnCode(),
                grn.getPurchaseOrderId(), totalValue));

        return buildResponse(grn);
    }

    @Override
    @Transactional
    public void cancel(String id, String reason) {
        GoodsReceiptNote grn = grnRepository.findById(id)
                .orElseThrow(() -> new QTHTException("goods.receipt.note.not.found", HttpStatus.NOT_FOUND));

        if ("CONFIRMED".equals(grn.getStatus())) {
            throw new QTHTException("goods.receipt.note.cannot.cancel.confirmed", HttpStatus.BAD_REQUEST);
        }

        grn.setStatus("CANCELLED");
        grn.setNote(reason);
        grnRepository.save(grn);
    }

    @Override
    @Transactional
    public void delete(String id) {
        GoodsReceiptNote grn = grnRepository.findById(id)
                .orElseThrow(() -> new QTHTException("goods.receipt.note.not.found", HttpStatus.NOT_FOUND));
        if ("CONFIRMED".equals(grn.getStatus())) {
            throw new QTHTException("goods.receipt.note.cannot.delete.confirmed", HttpStatus.BAD_REQUEST);
        }
        grnItemRepository.findByGrnId(id).forEach(grnItemRepository::delete);
        grnRepository.delete(grn);
    }

    private GrnResponse buildResponse(GoodsReceiptNote grn) {
        GrnResponse response = grnMapper.toResponse(grn);
        List<GoodsReceiptNoteItem> items = grnItemRepository.findByGrnId(grn.getId());
        response.setItems(items.stream().map(grnMapper::toItemResponse).toList());
        return response;
    }

    private GrnConfirmRequest.GrnConfirmItem findConfirmItem(GrnConfirmRequest request, String itemId) {
        if (request.getItems() == null) return null;
        return request.getItems().stream()
                .filter(i -> i.getItemId().equals(itemId))
                .findFirst().orElse(null);
    }

    private void updateStockBalance(String productId, String warehouseId, String locationId, String batchId, double qty) {
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

        balance.setQuantityOnHand(balance.getQuantityOnHand() + qty);
        balance.setQuantityAvailable(balance.getQuantityOnHand() - balance.getQuantityReserved());
        stockBalanceRepository.save(balance);
    }
}
