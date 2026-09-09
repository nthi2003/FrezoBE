package com.frezo.warehouse.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.response.PageResponse;
import com.frezo.common.utils.SecureCodeGenerator;
import com.frezo.product.entity.Product;
import com.frezo.product.repository.ProductRepository;
import com.frezo.warehouse.common.WarehouseErrorCode;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final ProductRepository productRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Lấy thông tin phiếu xuất kho theo id và trả về dữ liệu phản hồi đầy đủ; báo lỗi nếu không tìm thấy.
     */
    @Override
    public GinResponse getById(String id) {
        GoodsIssueNote gin = ginRepository.findById(id)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.GIN_NOT_FOUND));
        return buildResponse(gin);
    }

    /**
     * Lấy thông tin phiếu xuất kho theo mã phiếu và trả về dữ liệu phản hồi; báo lỗi nếu không tìm thấy.
     */
    @Override
    public GinResponse getByCode(String ginCode) {
        GoodsIssueNote gin = ginRepository.findByGinCode(ginCode)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.GIN_NOT_FOUND));
        return buildResponse(gin);
    }

    /**
     * Lọc danh sách phiếu xuất kho theo trạng thái và từ khóa, phân trang; trả về trang phản hồi.
     */
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

    /**
     * Tạo mới phiếu xuất kho: kiểm tra đủ hàng, sinh mã phiếu, lưu phiếu cùng các dòng và tính tổng giá trị.
     */
    @Override
    @Transactional
    public GinResponse create(GinCreateRequest request) {
        assertSufficientStockForCreate(request);

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

    /**
     * Trình phiếu xuất kho từ trạng thái DRAFT sang PENDING_APPROVAL sau khi xác nhận đủ tồn kho.
     */
    @Override
    @Transactional
    public GinResponse submit(String id) {
        GoodsIssueNote gin = ginRepository.findById(id)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.GIN_NOT_FOUND));
        if (!"DRAFT".equals(gin.getStatus())) {
            throw new AppException(WarehouseErrorCode.GIN_INVALID_STATUS);
        }
        assertSufficientStockForGinItems(gin.getWarehouseId(), ginItemRepository.findByGinId(id), true);
        gin.setStatus("PENDING_APPROVAL");
        ginRepository.save(gin);
        return buildResponse(gin);
    }

    /**
     * Duyệt phiếu xuất kho: chuyển trạng thái sang APPROVED và ghi nhận người duyệt, thời điểm duyệt.
     */
    @Override
    @Transactional
    public GinResponse approve(String id) {
        GoodsIssueNote gin = ginRepository.findById(id)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.GIN_NOT_FOUND));
        String st = gin.getStatus();
        if (!"PENDING_APPROVAL".equals(st) && !"DRAFT".equals(st)) {
            throw new AppException(WarehouseErrorCode.GIN_INVALID_STATUS);
        }
        assertSufficientStockForGinItems(gin.getWarehouseId(), ginItemRepository.findByGinId(id), true);
        gin.setStatus("APPROVED");
        gin.setApprovedBy(SystemUtils.getCurrentUsername());
        gin.setApprovedAt(LocalDateTime.now());
        ginRepository.save(gin);
        return buildResponse(gin);
    }

    /**
     * Xác nhận xuất kho: ghi nhận số lượng/lô/vị trí thực xuất, trừ tồn kho của lô, tạo sổ cái (ledger),
     * phiếu giao dịch và cập nhật trạng thái phiếu thành CONFIRMED.
     */
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

        // Resolve qty/batch trước — validate toàn bộ rồi mới trừ tồn
        List<ResolvedIssueLine> resolved = new ArrayList<>();
        for (GoodsIssueNoteItem item : items) {
            GinConfirmRequest.GinConfirmItem confirmItem = findConfirmItem(request, item.getId());
            double qtyIssued = confirmItem != null && confirmItem.getQtyIssued() != null
                    ? confirmItem.getQtyIssued()
                    : (item.getQtyIssued() != null ? item.getQtyIssued() : 0);
            String batchId = confirmItem != null && confirmItem.getBatchId() != null
                    ? confirmItem.getBatchId()
                    : item.getBatchId();
            String locationId = confirmItem != null && confirmItem.getLocationId() != null
                    ? confirmItem.getLocationId()
                    : item.getLocationId();
            resolved.add(new ResolvedIssueLine(item, qtyIssued, batchId, locationId));
        }
        assertSufficientStockForConfirm(gin.getWarehouseId(), resolved);

        List<StockLedger> ledgerEntries = new ArrayList<>();
        double totalValue = 0;

        for (ResolvedIssueLine line : resolved) {
            GoodsIssueNoteItem item = line.item();
            item.setBatchId(line.batchId());
            item.setLocationId(line.locationId());
            item.setQtyIssued(line.qtyIssued());
            ginItemRepository.save(item);

            if (line.qtyIssued() > 0) {
                deductBatchQty(line.batchId(), line.qtyIssued(), item.getProductId());

                StockLedger ledger = StockLedger.builder()
                        .productId(item.getProductId())
                        .batchId(line.batchId())
                        .locationId(line.locationId())
                        .transactionType("OUT")
                        .quantity(-line.qtyIssued())
                        .unitCost(item.getUnitCost())
                        .totalValue(-line.qtyIssued() * (item.getUnitCost() != null ? item.getUnitCost() : 0))
                        .referenceType("GIN")
                        .referenceId(gin.getId())
                        .note("Xuất kho từ phiếu " + gin.getGinCode())
                        .build();
                ledgerEntries.add(ledger);
                totalValue += line.qtyIssued() * (item.getUnitCost() != null ? item.getUnitCost() : 0);

                deductStockBalance(item.getProductId(), gin.getWarehouseId(),
                        line.locationId(), line.batchId(), line.qtyIssued());
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

    /**
     * Xác nhận xuất kho đồng loạt cho nhiều phiếu sau khi kiểm tra trạng thái hợp lệ của từng phiếu.
     */
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

    /**
     * Hủy phiếu xuất kho (không cho hủy phiếu đã CONFIRMED) và ghi lý do hủy.
     */
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

    /**
     * Hủy đồng loạt nhiều phiếu xuất kho với cùng một lý do.
     */
    @Override
    @Transactional
    public void batchCancel(List<String> ids, String reason) {
        for (String id : ids) {
            cancel(id, reason);
        }
    }

    /**
     * Xóa phiếu xuất kho kèm các dòng chi tiết; không cho xóa phiếu đã CONFIRMED.
     */
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

    // -------------------- Stock validation --------------------

    /**
     * Kiểm tra tồn kho đủ cho các dòng trong yêu cầu tạo phiếu; ném lỗi nếu thiếu hàng.
     */
    private void assertSufficientStockForCreate(GinCreateRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return;
        }
        // Gộp SL theo product (+ batch nếu có) — nhiều dòng cùng SP
        Map<String, Double> requestedByKey = new LinkedHashMap<>();
        Map<String, String> productIdByKey = new LinkedHashMap<>();
        Map<String, String> batchIdByKey = new LinkedHashMap<>();
        for (GinCreateRequest.GinItemRequest item : request.getItems()) {
            if (item.getProductId() == null || item.getProductId().isBlank()) continue;
            double qty = item.getQtyRequested() != null ? item.getQtyRequested() : 0;
            if (qty <= 0) continue;
            String batch = item.getBatchId() != null && !item.getBatchId().isBlank() ? item.getBatchId() : null;
            String key = item.getProductId() + "|" + (batch != null ? batch : "");
            requestedByKey.merge(key, qty, Double::sum);
            productIdByKey.put(key, item.getProductId());
            batchIdByKey.put(key, batch);
        }
        List<String> shortages = new ArrayList<>();
        for (Map.Entry<String, Double> e : requestedByKey.entrySet()) {
            String productId = productIdByKey.get(e.getKey());
            String batchId = batchIdByKey.get(e.getKey());
            double available = resolveAvailable(productId, request.getWarehouseId(), batchId);
            if (e.getValue() > available) {
                shortages.add(formatShortage(productId, batchId, e.getValue(), available));
            }
        }
        throwIfShortages(shortages);
    }

    /**
     * Kiểm tra tồn kho đủ cho danh sách dòng phiếu (theo số lượng yêu cầu hoặc xuất) trong một kho.
     */
    private void assertSufficientStockForGinItems(String warehouseId, List<GoodsIssueNoteItem> items, boolean useRequested) {
        Map<String, Double> requestedByKey = new LinkedHashMap<>();
        Map<String, String> productIdByKey = new LinkedHashMap<>();
        Map<String, String> batchIdByKey = new LinkedHashMap<>();
        for (GoodsIssueNoteItem item : items) {
            if (item.getProductId() == null) continue;
            double qty = useRequested
                    ? (item.getQtyRequested() != null ? item.getQtyRequested() : 0)
                    : (item.getQtyIssued() != null ? item.getQtyIssued() : 0);
            if (qty <= 0) continue;
            String batch = item.getBatchId() != null && !item.getBatchId().isBlank() ? item.getBatchId() : null;
            String key = item.getProductId() + "|" + (batch != null ? batch : "");
            requestedByKey.merge(key, qty, Double::sum);
            productIdByKey.put(key, item.getProductId());
            batchIdByKey.put(key, batch);
        }
        List<String> shortages = new ArrayList<>();
        for (Map.Entry<String, Double> e : requestedByKey.entrySet()) {
            String productId = productIdByKey.get(e.getKey());
            String batchId = batchIdByKey.get(e.getKey());
            double available = resolveAvailable(productId, warehouseId, batchId);
            if (e.getValue() > available) {
                shortages.add(formatShortage(productId, batchId, e.getValue(), available));
            }
        }
        throwIfShortages(shortages);
    }

    /**
     * Kiểm tra tồn kho đủ theo các dòng xuất đã được chốt lô/vị trí trước khi xác nhận xuất kho.
     */
    private void assertSufficientStockForConfirm(String warehouseId, List<ResolvedIssueLine> lines) {
        Map<String, Double> requestedByKey = new LinkedHashMap<>();
        Map<String, String> productIdByKey = new LinkedHashMap<>();
        Map<String, String> batchIdByKey = new LinkedHashMap<>();
        for (ResolvedIssueLine line : lines) {
            if (line.qtyIssued() <= 0) continue;
            String productId = line.item().getProductId();
            String batch = line.batchId() != null && !line.batchId().isBlank() ? line.batchId() : null;
            String key = productId + "|" + (batch != null ? batch : "");
            requestedByKey.merge(key, line.qtyIssued(), Double::sum);
            productIdByKey.put(key, productId);
            batchIdByKey.put(key, batch);
        }
        List<String> shortages = new ArrayList<>();
        for (Map.Entry<String, Double> e : requestedByKey.entrySet()) {
            String productId = productIdByKey.get(e.getKey());
            String batchId = batchIdByKey.get(e.getKey());
            double available = resolveAvailable(productId, warehouseId, batchId);
            if (e.getValue() > available) {
                shortages.add(formatShortage(productId, batchId, e.getValue(), available));
            }
        }
        throwIfShortages(shortages);
    }

    /**
     * Tính số lượng tồn khả dụng của sản phẩm theo kho và (tùy chọn) lô; ưu tiên giá trị available đã lưu.
     */
    private double resolveAvailable(String productId, String warehouseId, String batchId) {
        Double sum;
        if (batchId != null && !batchId.isBlank()) {
            sum = stockBalanceRepository.sumAvailableByProductWarehouseAndBatch(productId, warehouseId, batchId);
            if (sum == null || sum <= 0) {
                Double onHand = stockBalanceRepository.sumOnHandByProductWarehouseAndBatch(productId, warehouseId, batchId);
                Double reserved = stockBalanceRepository.sumReservedByProductWarehouseAndBatch(productId, warehouseId, batchId);
                return Math.max(0, nz(onHand) - nz(reserved));
            }
            return sum;
        }
        sum = stockBalanceRepository.sumAvailableByProductAndWarehouse(productId, warehouseId);
        if (sum == null || sum <= 0) {
            Double onHand = stockBalanceRepository.sumOnHandByProductAndWarehouse(productId, warehouseId);
            Double reserved = stockBalanceRepository.sumReservedByProductAndWarehouse(productId, warehouseId);
            return Math.max(0, nz(onHand) - nz(reserved));
        }
        return sum;
    }

    /**
     * Định dạng thông báo thiếu hàng gồm tên/mã sản phẩm, lô và số lượng yêu cầu, khả dụng, thiếu hụt.
     */
    private String formatShortage(String productId, String batchId, double requested, double available) {
        Product p = productRepository.findById(productId).orElse(null);
        String code = p != null && p.getCode() != null ? p.getCode() : productId;
        String name = p != null && p.getName() != null ? p.getName() : "";
        String label = name.isBlank() ? code : code + " (" + name + ")";
        String batchPart = batchId != null && !batchId.isBlank() ? ", lô " + batchId : "";
        double shortfall = requested - available;
        return String.format("%s%s: yêu cầu %s, khả dụng %s (thiếu %s)",
                label, batchPart, trimQty(requested), trimQty(available), trimQty(shortfall));
    }

    /**
     * Ném lỗi thiếu tồn kho nếu danh sách thiếu hụt không rỗng.
     */
    private void throwIfShortages(List<String> shortages) {
        if (shortages == null || shortages.isEmpty()) return;
        throw new AppException(WarehouseErrorCode.STOCK_BALANCE_INSUFFICIENT, String.join("; ", shortages));
    }

    /**
     * Loại bỏ phần thập phân thừa khi số lượng là số nguyên để hiển thị gọn gàng.
     */
    private static String trimQty(double v) {
        if (Math.rint(v) == v) return String.valueOf((long) v);
        return String.valueOf(v);
    }

    /**
     * Chuyển giá trị null thành 0.0 để tránh lỗi khi tính tổng.
     */
    private static double nz(Double v) {
        return v != null ? v : 0.0;
    }

    /**
     * Dựng đối tượng phản hồi phiếu xuất kho kèm tên kho, khách hàng, người thực hiện và danh sách dòng.
     */
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
        response.setCreatedByName(resolveUserDisplayName(gin.getCreatedBy()));
        response.setApprovedByName(resolveUserDisplayName(gin.getApprovedBy()));
        response.setIssuedByName(resolveUserDisplayName(gin.getIssuedBy()));
        List<GoodsIssueNoteItem> items = ginItemRepository.findByGinId(gin.getId());
        response.setItems(items.stream().map(ginMapper::toItemResponse).toList());
        return response;
    }

    /**
     * Display name: person.name → users.name → username.
     * Audit stores user id (UUID); legacy rows may store username — match both.
     */
    private String resolveUserDisplayName(String userRef) {
        if (userRef == null || userRef.isBlank()) {
            return null;
        }

        try {
            String name = jdbcTemplate.queryForObject(
                    """
                    SELECT COALESCE(
                        NULLIF(TRIM(p.name), ''),
                        NULLIF(TRIM(u.name), ''),
                        u.user_name
                    )
                    FROM users u
                    LEFT JOIN person p ON p.id = u.person_id
                        AND COALESCE(p.is_deleted, false) = false
                    WHERE (u.id = ? OR u.user_name = ?)
                      AND COALESCE(u.is_deleted, false) = false
                    LIMIT 1
                    """,
                    String.class, userRef, userRef);
            return name != null && !name.isBlank() ? name : userRef;
        } catch (EmptyResultDataAccessException ex) {
            return userRef;
        } catch (Exception ex) {
            log.debug("User display name enrich skip for {}: {}", userRef, ex.getMessage());
            return userRef;
        }
    }

    /**
     * Tra tên khách hàng theo id; thử bảng customers rồi fallback sang bảng khs.
     */
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

    /**
     * Tìm phần tử xác nhận tương ứng với một dòng phiếu trong yêu cầu xác nhận; trả null nếu không có.
     */
    private GinConfirmRequest.GinConfirmItem findConfirmItem(GinConfirmRequest request, String itemId) {
        if (request == null || request.getItems() == null) return null;
        return request.getItems().stream()
                .filter(i -> i.getItemId() != null && i.getItemId().equals(itemId))
                .findFirst().orElse(null);
    }

    /**
     * Trừ số lượng xuất khỏi tồn kho (balance) của sản phẩm theo kho/vị trí/lô sau khi kiểm tra đủ hàng.
     */
    private void deductStockBalance(String productId, String warehouseId, String locationId, String batchId, double qty) {
        StockBalance balance = stockBalanceRepository
                .findByProductIdAndWarehouseIdAndLocationIdAndBatchId(
                        productId, warehouseId, locationId, batchId)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.STOCK_BALANCE_NOT_FOUND));

        double onHand = balance.getQuantityOnHand() != null ? balance.getQuantityOnHand() : 0;
        double reserved = balance.getQuantityReserved() != null ? balance.getQuantityReserved() : 0;
        double available = balance.getQuantityAvailable() != null
                ? balance.getQuantityAvailable()
                : Math.max(0, onHand - reserved);

        if (available < qty || onHand < qty) {
            throw new AppException(WarehouseErrorCode.STOCK_BALANCE_INSUFFICIENT,
                    formatShortage(productId, batchId, qty, available));
        }

        balance.setQuantityOnHand(onHand - qty);
        balance.setQuantityAvailable(balance.getQuantityOnHand() - reserved);
        stockBalanceRepository.save(balance);
    }

    /**
     * Trừ số lượng xuất khỏi tồn của lô; đánh dấu lô DEPLETED khi hết hàng.
     */
    private void deductBatchQty(String batchId, double qty, String productId) {
        if (batchId == null || batchId.isBlank()) return;
        StockBatch batch = stockBatchRepository.findById(batchId)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.BATCH_NOT_FOUND));
        double onHand = batch.getQtyOnHand() != null ? batch.getQtyOnHand() : 0;
        if (onHand < qty) {
            throw new AppException(WarehouseErrorCode.BATCH_INSUFFICIENT,
                    formatShortage(productId != null ? productId : batch.getProductId(), batchId, qty, onHand));
        }
        batch.setQtyOnHand(onHand - qty);
        if (batch.getQtyOnHand() <= 0) {
            batch.setStatus("DEPLETED");
        }
        stockBatchRepository.save(batch);
    }

    private record ResolvedIssueLine(
            GoodsIssueNoteItem item,
            double qtyIssued,
            String batchId,
            String locationId
    ) {}
}
