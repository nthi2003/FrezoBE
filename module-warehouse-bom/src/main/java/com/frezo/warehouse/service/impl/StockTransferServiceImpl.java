package com.frezo.warehouse.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.response.PageResponse;
import com.frezo.common.utils.SecureCodeGenerator;
import com.frezo.warehouse.dto.request.TransferConfirmRequest;
import com.frezo.warehouse.dto.request.TransferCreateRequest;
import com.frezo.warehouse.dto.response.TransferResponse;
import com.frezo.warehouse.entity.*;
import com.frezo.warehouse.mapper.StockTransferMapper;
import com.frezo.warehouse.repository.*;
import com.frezo.warehouse.service.StockTransferService;
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
public class StockTransferServiceImpl implements StockTransferService {

    private final StockTransferRepository transferRepository;
    private final StockTransferItemRepository transferItemRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockTransferMapper transferMapper;

    @Override
    public TransferResponse getById(String id) {
        StockTransfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new QTHTException("stock.transfer.not.found", HttpStatus.NOT_FOUND));
        return buildResponse(transfer);
    }

    @Override
    public TransferResponse getByCode(String transferCode) {
        StockTransfer transfer = transferRepository.findByTransferCode(transferCode)
                .orElseThrow(() -> new QTHTException("stock.transfer.not.found", HttpStatus.NOT_FOUND));
        return buildResponse(transfer);
    }

    @Override
    public PageResponse<TransferResponse> filter(String status, String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<StockTransfer> transferPage;
        if (status != null && !status.isBlank()) {
            transferPage = transferRepository.findAll((root, query, cb) ->
                    cb.equal(root.get("status"), status), pageable);
        } else {
            transferPage = transferRepository.findAll(pageable);
        }
        List<TransferResponse> responses = transferPage.getContent().stream().map(this::buildResponse).toList();
        return PageResponse.of(page, size, transferPage, responses);
    }

    @Override
    @Transactional
    public TransferResponse create(TransferCreateRequest request) {
        if (request.getFromWarehouseId().equals(request.getToWarehouseId())) {
            throw new QTHTException("stock.transfer.same.warehouse", HttpStatus.BAD_REQUEST);
        }

        StockTransfer transfer = transferMapper.toEntity(request);
        transfer.setTransferCode(SecureCodeGenerator.generateCode("TF"));
        transfer = transferRepository.save(transfer);

        if (request.getItems() != null) {
            double totalValue = 0;
            for (TransferCreateRequest.TransferItemRequest itemReq : request.getItems()) {
                StockTransferItem item = transferMapper.toItemEntity(itemReq);
                item.setTransferId(transfer.getId());
                transferItemRepository.save(item);
                if (item.getQtyTransferred() != null && item.getUnitCost() != null) {
                    totalValue += item.getQtyTransferred() * item.getUnitCost();
                }
            }
            transfer.setTotalValue(totalValue);
            transferRepository.save(transfer);
        }

        return buildResponse(transfer);
    }

    @Override
    @Transactional
    public TransferResponse confirm(String id, TransferConfirmRequest request) {
        StockTransfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new QTHTException("stock.transfer.not.found", HttpStatus.NOT_FOUND));

        if (!"DRAFT".equals(transfer.getStatus())) {
            throw new QTHTException("stock.transfer.already.confirmed", HttpStatus.BAD_REQUEST);
        }

        List<StockTransferItem> items = transferItemRepository.findByTransferId(id);
        List<StockLedger> ledgerEntries = new ArrayList<>();
        double totalValue = 0;

        for (StockTransferItem item : items) {
            TransferConfirmRequest.TransferConfirmItem confirmItem = findConfirmItem(request, item.getId());
            double qty = confirmItem != null ? confirmItem.getQtyTransferred() : item.getQtyTransferred();

            item.setQtyTransferred(qty);
            if (confirmItem != null && confirmItem.getFromLocationId() != null) {
                item.setFromLocationId(confirmItem.getFromLocationId());
            }
            if (confirmItem != null && confirmItem.getToLocationId() != null) {
                item.setToLocationId(confirmItem.getToLocationId());
            }
            transferItemRepository.save(item);

            if (qty > 0) {
                deductStockBalance(item.getProductId(), transfer.getFromWarehouseId(),
                        item.getFromLocationId(), item.getBatchId(), qty);

                addStockBalance(item.getProductId(), transfer.getToWarehouseId(),
                        item.getToLocationId(), item.getBatchId(), qty);

                StockLedger outLedger = StockLedger.builder()
                        .productId(item.getProductId())
                        .batchId(item.getBatchId())
                        .warehouseId(transfer.getFromWarehouseId())
                        .locationId(item.getFromLocationId())
                        .transactionType("OUT")
                        .quantity(-qty)
                        .unitCost(item.getUnitCost())
                        .totalValue(-qty * (item.getUnitCost() != null ? item.getUnitCost() : 0))
                        .referenceType("TRANSFER")
                        .referenceId(transfer.getId())
                        .note("Chuyển kho từ phiếu " + transfer.getTransferCode())
                        .build();
                ledgerEntries.add(outLedger);

                StockLedger inLedger = StockLedger.builder()
                        .productId(item.getProductId())
                        .batchId(item.getBatchId())
                        .warehouseId(transfer.getToWarehouseId())
                        .locationId(item.getToLocationId())
                        .transactionType("IN")
                        .quantity(qty)
                        .unitCost(item.getUnitCost())
                        .totalValue(qty * (item.getUnitCost() != null ? item.getUnitCost() : 0))
                        .referenceType("TRANSFER")
                        .referenceId(transfer.getId())
                        .note("Nhập từ chuyển kho " + transfer.getTransferCode())
                        .build();
                ledgerEntries.add(inLedger);

                totalValue += qty * (item.getUnitCost() != null ? item.getUnitCost() : 0);
            }
        }

        stockLedgerRepository.saveAll(ledgerEntries);

        StockTransaction transaction = StockTransaction.builder()
                .transactionCode(SecureCodeGenerator.generateCode("ST"))
                .transactionType("TRANSFER")
                .referenceType("TRANSFER")
                .referenceId(transfer.getId())
                .warehouseId(transfer.getFromWarehouseId())
                .totalAmount(totalValue)
                .status("CONFIRMED")
                .note("Chuyển kho " + transfer.getTransferCode()
                      + " từ " + transfer.getFromWarehouseId()
                      + " đến " + transfer.getToWarehouseId())
                .build();
        stockTransactionRepository.save(transaction);

        transfer.setStatus("CONFIRMED");
        transfer.setTotalValue(totalValue);
        transfer.setTransferredBy(transfer.getCreatedBy());
        transfer.setTransferredAt(LocalDateTime.now());
        transferRepository.save(transfer);

        return buildResponse(transfer);
    }

    @Override
    @Transactional
    public void cancel(String id, String reason) {
        StockTransfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new QTHTException("stock.transfer.not.found", HttpStatus.NOT_FOUND));

        if ("CONFIRMED".equals(transfer.getStatus())) {
            throw new QTHTException("stock.transfer.cannot.cancel.confirmed", HttpStatus.BAD_REQUEST);
        }

        transfer.setStatus("CANCELLED");
        transfer.setNote(reason);
        transferRepository.save(transfer);
    }

    @Override
    @Transactional
    public void delete(String id) {
        StockTransfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new QTHTException("stock.transfer.not.found", HttpStatus.NOT_FOUND));
        if ("CONFIRMED".equals(transfer.getStatus())) {
            throw new QTHTException("stock.transfer.cannot.delete.confirmed", HttpStatus.BAD_REQUEST);
        }
        transferItemRepository.findByTransferId(id).forEach(transferItemRepository::delete);
        transferRepository.delete(transfer);
    }

    private TransferResponse buildResponse(StockTransfer transfer) {
        TransferResponse response = transferMapper.toResponse(transfer);
        List<StockTransferItem> items = transferItemRepository.findByTransferId(transfer.getId());
        response.setItems(items.stream().map(transferMapper::toItemResponse).toList());
        return response;
    }

    private TransferConfirmRequest.TransferConfirmItem findConfirmItem(TransferConfirmRequest request, String itemId) {
        if (request.getItems() == null) return null;
        return request.getItems().stream()
                .filter(i -> i.getItemId().equals(itemId))
                .findFirst().orElse(null);
    }

    private void deductStockBalance(String productId, String warehouseId, String locationId, String batchId, double qty) {
        StockBalance balance = stockBalanceRepository
                .findByProductIdAndWarehouseIdAndLocationIdAndBatchId(
                        productId, warehouseId, locationId, batchId)
                .orElseThrow(() -> new QTHTException("stock.balance.not.found", HttpStatus.NOT_FOUND));

        if (balance.getQuantityOnHand() < qty) {
            throw new QTHTException("stock.balance.insufficient", HttpStatus.BAD_REQUEST);
        }

        balance.setQuantityOnHand(balance.getQuantityOnHand() - qty);
        balance.setQuantityAvailable(balance.getQuantityOnHand() - balance.getQuantityReserved());
        stockBalanceRepository.save(balance);
    }

    private void addStockBalance(String productId, String warehouseId, String locationId, String batchId, double qty) {
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
