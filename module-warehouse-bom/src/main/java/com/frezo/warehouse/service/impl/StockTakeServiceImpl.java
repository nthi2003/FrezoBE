package com.frezo.warehouse.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.warehouse.dto.request.StockTakeLineRequest;
import com.frezo.warehouse.dto.request.StockTakeRequest;
import com.frezo.warehouse.dto.response.StockTakeLineResponse;
import com.frezo.warehouse.dto.response.StockTakeResponse;
import com.frezo.warehouse.entity.StockTake;
import com.frezo.warehouse.entity.StockTakeLine;
import com.frezo.warehouse.repository.StockBalanceRepository;
import com.frezo.warehouse.repository.StockTakeLineRepository;
import com.frezo.warehouse.repository.StockTakeRepository;
import com.frezo.warehouse.service.StockTakeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockTakeServiceImpl implements StockTakeService {

    private final StockTakeRepository stockTakeRepository;
    private final StockTakeLineRepository lineRepository;
    private final StockBalanceRepository stockBalanceRepository;

    @Override
    public List<StockTakeResponse> list(String warehouseId) {
        List<StockTake> list = warehouseId != null
                ? stockTakeRepository.findByWarehouseIdAndIsDeletedFalse(warehouseId)
                : stockTakeRepository.findByIsDeletedFalseOrderByCreatedDateDesc();
        return list.stream().map(this::toDto).toList();
    }

    @Override
    public StockTakeResponse get(String id) {
        return toDto(find(id));
    }

    @Override
    @Transactional
    public StockTakeResponse create(StockTakeRequest req) {
        StockTake st = StockTake.builder()
                .code("ST-" + System.currentTimeMillis())
                .warehouseId(req.getWarehouseId())
                .takeDate(req.getTakeDate() != null ? req.getTakeDate() : LocalDate.now())
                .status("DRAFT")
                .note(req.getNote())
                .build();
        st.setId(UUID.randomUUID().toString());
        st = stockTakeRepository.save(st);
        if (req.getLines() != null) {
            for (StockTakeLineRequest lr : req.getLines()) {
                Double sys = stockBalanceRepository
                        .sumAvailableByProductAndWarehouse(lr.getProductId(), req.getWarehouseId());
                double systemQty = sys != null ? sys : 0.0;
                StockTakeLine line = StockTakeLine.builder()
                        .stockTakeId(st.getId())
                        .productId(lr.getProductId())
                        .systemQty(systemQty)
                        .countedQty(null)
                        .varianceQty(null)
                        .note(lr.getNote())
                        .build();
                line.setId(UUID.randomUUID().toString());
                lineRepository.save(line);
            }
        }
        return toDto(st);
    }

    @Override
    @Transactional
    public StockTakeResponse start(String id) {
        StockTake st = find(id);
        if (!"DRAFT".equals(st.getStatus())) {
            throw new AppException(CommonErrorCode.CONFLICT, "Chỉ DRAFT mới start được");
        }
        st.setStatus("IN_PROGRESS");
        st.setStartedAt(LocalDateTime.now());
        return toDto(stockTakeRepository.save(st));
    }

    @Override
    @Transactional
    public StockTakeResponse submitCounted(String id, List<StockTakeLineRequest> lines) {
        StockTake st = find(id);
        if (!"IN_PROGRESS".equals(st.getStatus()) && !"DRAFT".equals(st.getStatus())) {
            throw new AppException(CommonErrorCode.CONFLICT, "Trạng thái không cho phép submit counted");
        }
        if (lines != null) {
            for (StockTakeLineRequest lr : lines) {
                if (lr.getId() == null) continue;
                lineRepository.findById(lr.getId()).ifPresent(line -> {
                    line.setCountedQty(lr.getCountedQty());
                    double sys = line.getSystemQty() != null ? line.getSystemQty() : 0.0;
                    double cnt = lr.getCountedQty() != null ? lr.getCountedQty() : 0.0;
                    line.setVarianceQty(cnt - sys);
                    if (lr.getNote() != null) line.setNote(lr.getNote());
                    lineRepository.save(line);
                });
            }
        }
        st.setStatus("SUBMITTED");
        st.setSubmittedAt(LocalDateTime.now());
        return toDto(stockTakeRepository.save(st));
    }

    @Override
    @Transactional
    public StockTakeResponse postVariance(String id) {
        StockTake st = find(id);
        if (!"SUBMITTED".equals(st.getStatus())) {
            throw new AppException(CommonErrorCode.CONFLICT, "Chỉ SUBMITTED mới post variance");
        }
        // Stub — log variance; inventory adjust thật sẽ nối StockAdjustment sau.
        List<StockTakeLine> lines = lineRepository.findByStockTakeIdAndIsDeletedFalse(id);
        for (StockTakeLine line : lines) {
            if (line.getVarianceQty() != null && line.getVarianceQty() != 0.0) {
                log.info("[StockTake] variance stub product={} wh={} qty={}",
                        line.getProductId(), st.getWarehouseId(), line.getVarianceQty());
            }
        }
        st.setStatus("POSTED");
        st.setPostedAt(LocalDateTime.now());
        return toDto(stockTakeRepository.save(st));
    }

    private StockTake find(String id) {
        return stockTakeRepository.findById(id)
                .filter(s -> Boolean.FALSE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Stock take không tồn tại"));
    }

    private StockTakeResponse toDto(StockTake st) {
        List<StockTakeLineResponse> lines = lineRepository.findByStockTakeIdAndIsDeletedFalse(st.getId())
                .stream()
                .map(l -> StockTakeLineResponse.builder()
                        .id(l.getId()).productId(l.getProductId())
                        .systemQty(l.getSystemQty()).countedQty(l.getCountedQty())
                        .varianceQty(l.getVarianceQty()).note(l.getNote()).build())
                .toList();
        return StockTakeResponse.builder()
                .id(st.getId()).code(st.getCode()).warehouseId(st.getWarehouseId())
                .takeDate(st.getTakeDate()).status(st.getStatus()).note(st.getNote())
                .lines(lines).build();
    }
}
