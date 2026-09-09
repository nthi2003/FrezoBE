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
import com.frezo.warehouse.entity.Warehouse;
import com.frezo.warehouse.repository.GoodsReceiptNoteRepository;
import com.frezo.warehouse.repository.PurchaseOrderLineRepository;
import com.frezo.warehouse.repository.PurchaseOrderRepository;
import com.frezo.warehouse.repository.PurchaseRequestLineRepository;
import com.frezo.warehouse.repository.PurchaseRequestRepository;
import com.frezo.warehouse.repository.StockBalanceRepository;
import com.frezo.warehouse.repository.WarehouseRepository;
import com.frezo.warehouse.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderLineRepository poLineRepository;
    private final PurchaseRequestRepository prRepository;
    private final PurchaseRequestLineRepository prLineRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final WarehouseRepository warehouseRepository;
    private final GoodsReceiptNoteRepository grnRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Liệt kê toàn bộ đơn đặt hàng chưa xóa, sắp xếp theo thời gian tạo giảm dần.
     */
    @Override
    public FePage<PurchaseOrderDto> list() {
        return FePage.all(poRepository.findByIsDeletedFalseOrderByCreatedDateDesc().stream()
                .map(this::toDto).toList());
    }

    /**
     * Lấy chi tiết đơn đặt hàng theo id và chuyển thành dữ liệu phản hồi.
     */
    @Override
    public PurchaseOrderDto get(String id) {
        return toDto(find(id));
    }

    /**
     * Tạo mới đơn đặt hàng ở trạng thái DRAFT: sinh mã, lưu đơn và các dòng chi tiết.
     */
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

    /**
     * Cập nhật đơn đặt hàng (không cho sửa PO đã RECEIVED); xóa dòng cũ và lưu lại các dòng mới.
     */
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

    /**
     * Xóa mềm đơn đặt hàng và các dòng chi tiết; không cho xóa PO đã RECEIVED.
     */
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

    /**
     * Tạo đơn đặt hàng từ yêu cầu mua đã APPROVED; nếu đã có PO cho PR đó thì trả về PO hiện có (đảm bảo idempotent).
     */
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

    /**
     * Xác nhận đã nhận hàng cho PO: ghi nhận số lượng nhận từng dòng, tăng tồn kho và đánh dấu PO là RECEIVED.
     */
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

    /**
     * Tăng tồn kho (balance) của sản phẩm theo kho khi nhận hàng; tạo bản ghi balance mới nếu chưa có.
     */
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

    /**
     * Lưu danh sách các dòng chi tiết vào một đơn đặt hàng.
     */
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

    /**
     * Sinh mã đơn đặt hàng duy nhất dựa trên timestamp hiện tại.
     */
    private String nextCode() {
        return "PO-" + System.currentTimeMillis();
    }

    /**
     * Tìm đơn đặt hàng chưa xóa theo id; ném lỗi nếu không tồn tại.
     */
    private PurchaseOrder find(String id) {
        return poRepository.findById(id)
                .filter(p -> Boolean.FALSE.equals(p.getIsDeleted()) || p.getIsDeleted() == null)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "PO không tồn tại"));
    }

    /**
     * Chuyển đối tượng đơn đặt hàng thành DTO phản hồi kèm các dòng, tên nhà cung cấp, kho và GRN liên kết.
     */
    private PurchaseOrderDto toDto(PurchaseOrder po) {
        List<PurchaseOrderLineDto> lines = poLineRepository
                .findByPurchaseOrderIdAndIsDeletedFalse(po.getId()).stream()
                .map(this::toLineDto)
                .toList();
        String created = po.getCreatedDate() != null ? po.getCreatedDate().format(ISO) : null;
        return PurchaseOrderDto.builder()
                .id(po.getId())
                .code(po.getCode())
                .prId(po.getPrId())
                .purchaseRequestId(po.getPrId())
                .supplierId(po.getSupplierId())
                .supplierName(resolveSupplierName(po.getSupplierId()))
                .warehouseId(po.getWarehouseId())
                .warehouseName(resolveWarehouseName(po.getWarehouseId()))
                .status(po.getStatus())
                .note(po.getNote())
                .confirmedAt(po.getConfirmedAt() != null ? po.getConfirmedAt().format(ISO) : null)
                .receivedAt(po.getReceivedAt() != null ? po.getReceivedAt().format(ISO) : null)
                .createdDate(created)
                .createdAt(created)
                .lines(lines)
                .linkedGrns(resolveLinkedGrns(po.getId()))
                .build();
    }

    /**
     * Chuyển một dòng đơn đặt hàng thành DTO phản hồi kèm thông tin sản phẩm và đơn giá từ GRN.
     */
    private PurchaseOrderLineDto toLineDto(PurchaseOrderLine l) {
        PurchaseOrderLineDto dto = PurchaseOrderLineDto.builder()
                .id(l.getId())
                .prLineId(l.getPrLineId())
                .productId(l.getProductId())
                .warehouseId(l.getWarehouseId())
                .qty(l.getQty())
                .qtyOrdered(l.getQty())
                .receivedQty(l.getReceivedQty())
                .qtyReceived(l.getReceivedQty())
                .note(l.getNote())
                .build();
        enrichProductFields(dto);
        enrichUnitPriceFromGrn(dto, l.getPurchaseOrderId(), l.getProductId());
        return dto;
    }

    /**
     * Bổ sung mã và tên sản phẩm vào dòng phản hồi qua truy vấn JDBC.
     */
    private void enrichProductFields(PurchaseOrderLineDto item) {
        if (item.getProductId() == null || item.getProductId().isBlank()) {
            return;
        }
        try {
            jdbcTemplate.query(
                    "SELECT code, name FROM products WHERE id = ? LIMIT 1",
                    rs -> {
                        item.setProductCode(rs.getString("code"));
                        item.setProductName(rs.getString("name"));
                    },
                    item.getProductId());
        } catch (Exception ex) {
            log.debug("Product enrich skip for {}", item.getProductId());
        }
    }

    /** Lấy đơn giá từ dòng GRN gắn PO (nếu đã nhập kho). */
    private void enrichUnitPriceFromGrn(PurchaseOrderLineDto item, String poId, String productId) {
        if (poId == null || productId == null) {
            return;
        }
        try {
            Double unitCost = jdbcTemplate.queryForObject(
                    """
                    SELECT gi.unit_cost
                    FROM goods_receipt_note_items gi
                    JOIN goods_receipt_notes g ON g.id = gi.grn_id
                    WHERE g.purchase_order_id = ?
                      AND gi.product_id = ?
                      AND gi.unit_cost IS NOT NULL
                      AND COALESCE(gi.is_deleted, false) = false
                      AND COALESCE(g.is_deleted, false) = false
                    ORDER BY g.created_date DESC
                    LIMIT 1
                    """,
                    Double.class, poId, productId);
            item.setUnitPrice(unitCost);
        } catch (EmptyResultDataAccessException ex) {
            // PO chưa có GRN — không có đơn giá
        } catch (Exception ex) {
            log.debug("Unit price enrich skip for PO {} product {}: {}", poId, productId, ex.getMessage());
        }
    }

    /**
     * Truy xuất các GRN chưa xóa liên kết với một đơn đặt hàng và tóm tắt thông tin từng phiếu.
     */
    private List<PurchaseOrderDto.LinkedGrnSummary> resolveLinkedGrns(String poId) {
        if (poId == null || poId.isBlank()) {
            return List.of();
        }
        return grnRepository.findByPurchaseOrderId(poId).stream()
                .filter(g -> Boolean.FALSE.equals(g.getIsDeleted()) || g.getIsDeleted() == null)
                .map(g -> PurchaseOrderDto.LinkedGrnSummary.builder()
                        .id(g.getId())
                        .grnCode(g.getGrnCode())
                        .status(g.getStatus())
                        .invoiceNo(g.getInvoiceNo())
                        .invoiceDate(g.getInvoiceDate() != null ? g.getInvoiceDate().format(ISO_DATE) : null)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Tra tên nhà cung cấp theo id từ bảng nccs; trả null nếu không tìm thấy.
     */
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

    /**
     * Tra tên kho theo id; trả null nếu không tìm thấy.
     */
    private String resolveWarehouseName(String warehouseId) {
        if (warehouseId == null || warehouseId.isBlank()) {
            return null;
        }
        return warehouseRepository.findById(warehouseId).map(Warehouse::getName).orElse(null);
    }
}
