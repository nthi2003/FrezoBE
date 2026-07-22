package com.frezo.qtbv.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.workflow.dto.WorkflowInstanceDto;
import com.frezo.common.workflow.dto.WorkflowTaskDto;
import com.frezo.common.workflow.service.WorkflowService;
import com.frezo.qtbv.dto.request.AssetAssignRequest;
import com.frezo.qtbv.dto.request.AssetSaveRequest;
import com.frezo.qtbv.dto.request.AssetTransferCreateRequest;
import com.frezo.qtbv.dto.response.AssetAssignmentResponse;
import com.frezo.qtbv.dto.response.AssetResponse;
import com.frezo.qtbv.dto.response.AssetTransferRequestResponse;
import com.frezo.qtbv.entity.Asset;
import com.frezo.qtbv.entity.AssetAssignment;
import com.frezo.qtbv.entity.AssetTransferRequest;
import com.frezo.qtbv.entity.Category;
import com.frezo.qtbv.repository.AssetAssignmentRepository;
import com.frezo.qtbv.repository.AssetRepository;
import com.frezo.qtbv.repository.AssetTransferRequestRepository;
import com.frezo.qtbv.repository.CategoryRepository;
import com.frezo.qtbv.service.AssetService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quản lý tài sản — CRUD + workflow cấp phát / bảo trì / thanh lý.
 *
 * <h3>State transitions</h3>
 * <pre>
 *   AVAILABLE ──(assign)──▶ IN_USE ──(unassign)──▶ AVAILABLE
 *       │                     │
 *       │                     └──(startMaintenance)─▶ MAINTENANCE ──(endMaintenance)─▶ AVAILABLE
 *       │
 *       ├──(dispose)────▶ DISPOSED  [terminal]
 *       └──(reportLost)─▶ LOST      [terminal]
 * </pre>
 *
 * Mỗi transition ghi 1 record {@link AssetAssignment} → timeline drawer FE.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssetServiceImpl implements AssetService {

    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_IN_USE = "IN_USE";
    private static final String STATUS_MAINTENANCE = "MAINTENANCE";
    private static final String STATUS_DISPOSED = "DISPOSED";

    // Workflow ticket
    private static final String TR_PENDING = "PENDING";
    private static final String TR_APPROVED = "APPROVED";
    private static final String TR_REJECTED = "REJECTED";
    private static final String TR_HANDED_OVER = "HANDED_OVER";
    private static final String TR_CANCELLED = "CANCELLED";
    private static final String TR_TYPE_ASSIGN = "ASSIGN";
    private static final String TR_TYPE_RETURN = "RETURN";

    // ---- Workflow engine binding ----
    /** entityType để nhận biết instance của ticket cấp phát tài sản trong engine. */
    public static final String WF_ENTITY_TYPE = "ASSET_TRANSFER";
    /** Definition code mặc định (seed sẵn trong {@code WorkflowDataInitializer}). */
    public static final String WF_DEF_CODE = "ASSET_TRANSFER_DEFAULT";

    private final AssetRepository assetRepository;
    private final AssetAssignmentRepository assignmentRepository;
    private final AssetTransferRequestRepository transferRequestRepository;
    private final CategoryRepository categoryRepository;
    /**
     * Không inject {@code PersonRepository} vì {@code module-dmdc-bom} là upstream —
     * nếu depend qtht-bom sẽ tạo cycle với customer-bom. Dùng {@link JdbcTemplate}
     * để query trực tiếp bảng {@code person} khi cần enrich tên nhân viên.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Workflow engine chung — nếu definition {@link #WF_DEF_CODE} tồn tại, ticket sẽ
     * được lái bởi engine (approve/reject qua {@code WorkflowService}). Nếu không
     * (VD DB chưa migrate seeder), fallback về flow hard-coded 3-status cũ.
     */
    private final WorkflowService workflowService;

    // ============================================================
    // Query
    // ============================================================

    @Override
    public Map<String, Object> list(String keyword, String status, String categoryCode,
                                    String assignedPersonId, int page, int size) {
        Specification<Asset> spec = (root, q, cb) -> {
            List<Predicate> ands = new ArrayList<>();
            ands.add(cb.equal(root.get("isDeleted"), false));
            if (status != null && !status.isBlank()) {
                ands.add(cb.equal(root.get("status"), status));
            }
            if (categoryCode != null && !categoryCode.isBlank()) {
                ands.add(cb.equal(root.get("categoryCode"), categoryCode));
            }
            if (assignedPersonId != null && !assignedPersonId.isBlank()) {
                ands.add(cb.equal(root.get("assignedPersonId"), assignedPersonId));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase() + "%";
                ands.add(cb.or(
                        cb.like(cb.lower(root.get("code")), like),
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("brand")), like),
                        cb.like(cb.lower(root.get("model")), like),
                        cb.like(cb.lower(root.get("serialNumber")), like)
                ));
            }
            return cb.and(ands.toArray(new Predicate[0]));
        };

        Page<Asset> pageResult = assetRepository.findAll(spec,
                ServiceHelper.createPageable(page, size, Sort.by(Sort.Direction.DESC, "createdDate")));

        List<AssetResponse> items = pageResult.getContent().stream().map(this::enrich).toList();
        Map<String, Object> resp = new HashMap<>();
        resp.put("pageNumber", page);
        resp.put("pageSize", size);
        resp.put("total", pageResult.getTotalElements());
        resp.put("items", items);
        return resp;
    }

    @Override
    public AssetResponse getById(String id) {
        return enrich(findOrThrow(id));
    }

    // ============================================================
    // CRUD
    // ============================================================

    @Override
    @Transactional
    public AssetResponse create(AssetSaveRequest req) {
        Asset e = Asset.builder()
                .code((req.getCode() != null && !req.getCode().isBlank())
                        ? req.getCode().trim().toUpperCase() : generateCode())
                .name(req.getName())
                .categoryCode(req.getCategoryCode())
                .brand(req.getBrand())
                .model(req.getModel())
                .serialNumber(req.getSerialNumber())
                .purchaseDate(req.getPurchaseDate())
                .purchasePrice(req.getPurchasePrice())
                .currentValue(req.getCurrentValue() != null ? req.getCurrentValue() : req.getPurchasePrice())
                .warrantyEndDate(req.getWarrantyEndDate())
                .status(req.getStatus() != null ? req.getStatus() : STATUS_AVAILABLE)
                .location(req.getLocation())
                .imageUrl(req.getImageUrl())
                .note(req.getNote())
                .build();
        if (assetRepository.existsByCode(e.getCode())) {
            throw new QTHTException("error.asset.code.duplicate");
        }
        Asset saved = assetRepository.save(e);
        return enrich(saved);
    }

    @Override
    @Transactional
    public AssetResponse update(String id, AssetSaveRequest req) {
        Asset e = findOrThrow(id);
        // Không cho đổi code
        if (req.getName() != null) e.setName(req.getName());
        if (req.getCategoryCode() != null) e.setCategoryCode(req.getCategoryCode());
        if (req.getBrand() != null) e.setBrand(req.getBrand());
        if (req.getModel() != null) e.setModel(req.getModel());
        if (req.getSerialNumber() != null) e.setSerialNumber(req.getSerialNumber());
        if (req.getPurchaseDate() != null) e.setPurchaseDate(req.getPurchaseDate());
        if (req.getPurchasePrice() != null) e.setPurchasePrice(req.getPurchasePrice());
        if (req.getCurrentValue() != null) e.setCurrentValue(req.getCurrentValue());
        if (req.getWarrantyEndDate() != null) e.setWarrantyEndDate(req.getWarrantyEndDate());
        if (req.getLocation() != null) e.setLocation(req.getLocation());
        if (req.getImageUrl() != null) e.setImageUrl(req.getImageUrl());
        if (req.getNote() != null) e.setNote(req.getNote());
        // Status chỉ đổi qua transition method (assign/unassign/dispose)
        return enrich(assetRepository.save(e));
    }

    @Override
    @Transactional
    public void delete(String id) {
        Asset e = findOrThrow(id);
        // Đang cấp phát không cho xoá
        if (STATUS_IN_USE.equals(e.getStatus())) {
            throw new QTHTException("error.asset.in.use.cannot.delete");
        }
        e.softDelete(SystemUtils.getCurrentUsername());
        assetRepository.save(e);
    }

    // ============================================================
    // Workflow transitions
    // ============================================================

    @Override
    @Transactional
    public AssetResponse assign(String id, AssetAssignRequest req) {
        Asset e = findOrThrow(id);
        if (!STATUS_AVAILABLE.equals(e.getStatus())) {
            throw new QTHTException("error.asset.not.available");
        }
        if (req.getPersonId() == null || req.getPersonId().isBlank()) {
            throw new QTHTException("error.asset.assign.person.required");
        }
        // Enrich personName nếu client không truyền
        String personName = req.getPersonName();
        if (personName == null || personName.isBlank()) {
            personName = lookupPersonName(req.getPersonId());
        }

        LocalDate date = req.getActionDate() != null ? req.getActionDate() : LocalDate.now();
        e.setStatus(STATUS_IN_USE);
        e.setAssignedPersonId(req.getPersonId());
        e.setAssignedAt(date);
        assetRepository.save(e);

        writeAssignment(e.getId(), "ASSIGN", req.getPersonId(), personName, date, req.getNote(), null);
        return enrich(e);
    }

    @Override
    @Transactional
    public AssetResponse unassign(String id, String note) {
        Asset e = findOrThrow(id);
        if (!STATUS_IN_USE.equals(e.getStatus())) {
            throw new QTHTException("error.asset.not.in.use");
        }
        String personId = e.getAssignedPersonId();
        String personName = personId != null ? lookupPersonName(personId) : null;

        e.setStatus(STATUS_AVAILABLE);
        e.setAssignedPersonId(null);
        e.setAssignedAt(null);
        assetRepository.save(e);

        writeAssignment(id, "RETURN", personId, personName, LocalDate.now(), note, null);
        return enrich(e);
    }

    @Override
    @Transactional
    public AssetResponse startMaintenance(String id, String note) {
        Asset e = findOrThrow(id);
        if (STATUS_DISPOSED.equals(e.getStatus())) {
            throw new QTHTException("error.asset.disposed");
        }
        e.setStatus(STATUS_MAINTENANCE);
        assetRepository.save(e);
        writeAssignment(id, "MAINTENANCE_START", null, null, LocalDate.now(), note, null);
        return enrich(e);
    }

    @Override
    @Transactional
    public AssetResponse endMaintenance(String id, String note, BigDecimal cost) {
        Asset e = findOrThrow(id);
        if (!STATUS_MAINTENANCE.equals(e.getStatus())) {
            throw new QTHTException("error.asset.not.in.maintenance");
        }
        e.setStatus(STATUS_AVAILABLE);
        assetRepository.save(e);
        writeAssignment(id, "MAINTENANCE_END", null, null, LocalDate.now(), note, cost);
        return enrich(e);
    }

    @Override
    @Transactional
    public AssetResponse dispose(String id, String note) {
        Asset e = findOrThrow(id);
        if (STATUS_DISPOSED.equals(e.getStatus())) {
            throw new QTHTException("error.asset.already.disposed");
        }
        e.setStatus(STATUS_DISPOSED);
        // Nếu đang cấp phát → auto-return trong lịch sử trước khi dispose
        if (e.getAssignedPersonId() != null) {
            String pid = e.getAssignedPersonId();
            String pn = lookupPersonName(pid);
            writeAssignment(id, "RETURN", pid, pn, LocalDate.now(), "Auto-return trước khi thanh lý", null);
            e.setAssignedPersonId(null);
            e.setAssignedAt(null);
        }
        assetRepository.save(e);
        writeAssignment(id, "DISPOSE", null, null, LocalDate.now(), note, null);
        return enrich(e);
    }

    // ============================================================
    // History + stats
    // ============================================================

    @Override
    public List<AssetAssignmentResponse> history(String assetId) {
        return assignmentRepository.findByAssetIdOrderByCreatedDateDesc(assetId).stream()
                .map(this::mapAssignment).toList();
    }

    @Override
    public Map<String, Object> stats() {
        List<Asset> all = assetRepository.findAll().stream()
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .toList();
        long total = all.size();
        long inUse = all.stream().filter(a -> STATUS_IN_USE.equals(a.getStatus())).count();
        long available = all.stream().filter(a -> STATUS_AVAILABLE.equals(a.getStatus())).count();
        long maintenance = all.stream().filter(a -> STATUS_MAINTENANCE.equals(a.getStatus())).count();
        long disposed = all.stream().filter(a -> STATUS_DISPOSED.equals(a.getStatus())).count();

        BigDecimal totalValue = all.stream()
                .filter(a -> !STATUS_DISPOSED.equals(a.getStatus()))
                .map(a -> a.getCurrentValue() != null ? a.getCurrentValue()
                        : (a.getPurchasePrice() != null ? a.getPurchasePrice() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Sắp hết bảo hành (< 30 ngày)
        LocalDate threshold = LocalDate.now().plusDays(30);
        long expiringSoon = all.stream()
                .filter(a -> a.getWarrantyEndDate() != null)
                .filter(a -> !a.getWarrantyEndDate().isBefore(LocalDate.now()))
                .filter(a -> a.getWarrantyEndDate().isBefore(threshold))
                .count();

        Map<String, Object> r = new HashMap<>();
        r.put("total", total);
        r.put("inUse", inUse);
        r.put("available", available);
        r.put("maintenance", maintenance);
        r.put("disposed", disposed);
        r.put("totalValue", totalValue);
        r.put("warrantyExpiringSoon", expiringSoon);
        return r;
    }

    // ============================================================
    // Helpers
    // ============================================================

    private Asset findOrThrow(String id) {
        return assetRepository.findById(id)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new QTHTException("error.asset.not.found"));
    }

    /**
     * Query trực tiếp bảng {@code person} vì không thể inject {@code PersonRepository}
     * (cross-module cycle). Trả null nếu không tồn tại — caller ignore.
     */
    private String lookupPersonName(String personId) {
        if (personId == null || personId.isBlank()) return null;
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT name FROM person WHERE id = ? AND (is_deleted IS NULL OR is_deleted = false)",
                    String.class, personId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            log.warn("[asset] Không lookup được person name id={}: {}", personId, e.getMessage());
            return null;
        }
    }

    /** Auto-gen code dạng AS-YYYY-####. Concurrent-safe hơn nếu wrap advisory lock, nhưng
     *  cho SMB (< 1K asset/năm) count query đủ dùng. */
    private String generateCode() {
        int year = LocalDate.now().getYear();
        String prefix = "AS-" + year + "-";
        long count = assetRepository.countByCodePrefix(prefix);
        // +1, retry vài lần nếu collision (edge case race)
        for (int i = 1; i <= 5; i++) {
            String candidate = prefix + String.format("%04d", count + i);
            if (!assetRepository.existsByCode(candidate)) return candidate;
        }
        // Fallback timestamp
        return prefix + System.currentTimeMillis() % 10000;
    }

    private void writeAssignment(String assetId, String action, String personId, String personName,
                                 LocalDate date, String note, BigDecimal cost) {
        try {
            assignmentRepository.save(AssetAssignment.builder()
                    .assetId(assetId)
                    .action(action)
                    .personId(personId)
                    .personName(personName)
                    .actionDate(date)
                    .note(note)
                    .cost(cost)
                    .build());
        } catch (Exception ex) {
            log.error("[asset] Không ghi được lịch sử: {}", ex.getMessage());
        }
    }

    private AssetResponse enrich(Asset e) {
        AssetResponse r = new AssetResponse();
        r.setId(e.getId());
        r.setCode(e.getCode());
        r.setName(e.getName());
        r.setCategoryCode(e.getCategoryCode());
        if (e.getCategoryCode() != null) {
            categoryRepository.findByCode(e.getCategoryCode()).map(Category::getName).ifPresent(r::setCategoryName);
        }
        r.setBrand(e.getBrand());
        r.setModel(e.getModel());
        r.setSerialNumber(e.getSerialNumber());
        r.setPurchaseDate(e.getPurchaseDate());
        r.setPurchasePrice(e.getPurchasePrice());
        r.setCurrentValue(e.getCurrentValue());
        r.setWarrantyEndDate(e.getWarrantyEndDate());
        r.setStatus(e.getStatus());
        r.setLocation(e.getLocation());
        r.setAssignedPersonId(e.getAssignedPersonId());
        if (e.getAssignedPersonId() != null) {
            String pn = lookupPersonName(e.getAssignedPersonId());
            if (pn != null) r.setAssignedPersonName(pn);
        }
        r.setAssignedAt(e.getAssignedAt());
        r.setImageUrl(e.getImageUrl());
        r.setNote(e.getNote());
        r.setCreatedBy(e.getCreatedBy());
        r.setCreatedDate(e.getCreatedDate() != null ? e.getCreatedDate().toString() : null);
        return r;
    }

    private AssetAssignmentResponse mapAssignment(AssetAssignment a) {
        AssetAssignmentResponse r = new AssetAssignmentResponse();
        r.setId(a.getId());
        r.setAssetId(a.getAssetId());
        r.setAction(a.getAction());
        r.setPersonId(a.getPersonId());
        r.setPersonName(a.getPersonName());
        r.setActionDate(a.getActionDate());
        r.setNote(a.getNote());
        r.setCost(a.getCost());
        r.setCreatedBy(a.getCreatedBy());
        r.setCreatedDate(a.getCreatedDate() != null ? a.getCreatedDate().toString() : null);
        return r;
    }

    // ============================================================
    // Workflow: Transfer Request (ticket cấp phát / thu hồi)
    // ============================================================

    @Override
    @Transactional
    public AssetTransferRequestResponse createTransferRequest(String assetId, AssetTransferCreateRequest req) {
        Asset asset = findOrThrow(assetId);
        String type = (req.getRequestType() != null && !req.getRequestType().isBlank())
                ? req.getRequestType().trim().toUpperCase() : TR_TYPE_ASSIGN;

        // Pre-check theo type + tránh 2 request đang chạy cùng lúc
        List<AssetTransferRequest> active = transferRequestRepository
                .findByAssetIdAndStatusIn(assetId, List.of(TR_PENDING, TR_APPROVED));
        if (!active.isEmpty()) {
            throw new QTHTException("error.asset.transfer.active.exists");
        }

        if (TR_TYPE_ASSIGN.equals(type)) {
            if (!STATUS_AVAILABLE.equals(asset.getStatus())) {
                throw new QTHTException("error.asset.not.available");
            }
            if (req.getPersonId() == null || req.getPersonId().isBlank()) {
                throw new QTHTException("error.asset.assign.person.required");
            }
        } else if (TR_TYPE_RETURN.equals(type)) {
            if (!STATUS_IN_USE.equals(asset.getStatus())) {
                throw new QTHTException("error.asset.not.in.use");
            }
            // Với RETURN, personId auto = người đang giữ
            req.setPersonId(asset.getAssignedPersonId());
        } else {
            throw new QTHTException("error.asset.transfer.type.invalid");
        }

        // Enrich personName nếu client không truyền
        String personName = req.getPersonName();
        if ((personName == null || personName.isBlank()) && req.getPersonId() != null) {
            personName = lookupPersonName(req.getPersonId());
        }

        AssetTransferRequest tr = AssetTransferRequest.builder()
                .assetId(assetId)
                .requestType(type)
                .status(TR_PENDING)
                .requesterUsername(SystemUtils.getCurrentUsername())
                .personId(req.getPersonId())
                .personName(personName)
                .reason(req.getReason())
                .plannedDate(req.getPlannedDate() != null ? req.getPlannedDate() : LocalDate.now())
                .build();
        AssetTransferRequest saved = transferRequestRepository.save(tr);

        // ---- Wire vào Workflow Engine (nếu definition tồn tại) ----
        // Ưu điểm: admin có thể sửa flow (thêm bước, đổi approver) qua /qtht/workflows
        // mà không cần đụng code business. Instance được tạo ngay để FE có state chạy engine.
        try {
            String title = String.format("%s %s cho %s",
                    TR_TYPE_ASSIGN.equals(type) ? "Cấp phát" : "Thu hồi",
                    asset.getName(),
                    personName != null ? personName : "N/A");
            WorkflowInstanceDto instance = workflowService.start(
                    WF_DEF_CODE, WF_ENTITY_TYPE, saved.getId(),
                    saved.getRequesterUsername(), title);
            saved.setWorkflowInstanceId(instance.getId());
            saved = transferRequestRepository.save(saved);
            log.info("[asset] Tạo ticket {} #{} + workflow instance {} cho asset {}",
                    type, saved.getId(), instance.getId(), asset.getCode());
        } catch (Exception ex) {
            // Fallback graceful — nếu engine chưa seed definition, ticket vẫn hoạt động
            // theo flow hard-coded 3-status như trước.
            log.warn("[asset] Không start được workflow cho ticket {}: {} — fallback legacy flow",
                    saved.getId(), ex.getMessage());
        }
        return enrichTransfer(saved, asset);
    }

    @Override
    @Transactional
    public AssetTransferRequestResponse approveTransferRequest(String requestId, String note) {
        AssetTransferRequest tr = findTransferOrThrow(requestId);

        // ---- Path A: có workflow engine — route qua engine ----
        if (tr.getWorkflowInstanceId() != null) {
            return approveViaEngine(tr, note);
        }

        // ---- Path B: legacy fallback ----
        requireApprover();
        if (!TR_PENDING.equals(tr.getStatus())) {
            throw new QTHTException("error.asset.transfer.not.pending");
        }
        tr.setStatus(TR_APPROVED);
        tr.setApprovedBy(SystemUtils.getCurrentUsername());
        tr.setApprovedAt(LocalDateTime.now());
        tr.setApproveNote(note);
        transferRequestRepository.save(tr);
        return enrichTransfer(tr, null);
    }

    @Override
    @Transactional
    public AssetTransferRequestResponse rejectTransferRequest(String requestId, String reason) {
        AssetTransferRequest tr = findTransferOrThrow(requestId);
        if (reason == null || reason.isBlank()) {
            throw new QTHTException("error.asset.transfer.reject.reason.required");
        }

        if (tr.getWorkflowInstanceId() != null) {
            WorkflowTaskDto task = findCurrentPendingTask(tr);
            if (task == null) throw new QTHTException("error.asset.transfer.no.pending.task");
            workflowService.rejectTask(task.getId(), reason.trim());
            tr.setStatus(TR_REJECTED);
            tr.setRejectedBy(SystemUtils.getCurrentUsername());
            tr.setRejectedAt(LocalDateTime.now());
            tr.setRejectReason(reason.trim());
            transferRequestRepository.save(tr);
            return enrichTransfer(tr, null);
        }

        requireApprover();
        if (!TR_PENDING.equals(tr.getStatus())) {
            throw new QTHTException("error.asset.transfer.not.pending");
        }
        tr.setStatus(TR_REJECTED);
        tr.setRejectedBy(SystemUtils.getCurrentUsername());
        tr.setRejectedAt(LocalDateTime.now());
        tr.setRejectReason(reason.trim());
        transferRequestRepository.save(tr);
        return enrichTransfer(tr, null);
    }

    @Override
    @Transactional
    public AssetTransferRequestResponse cancelTransferRequest(String requestId) {
        AssetTransferRequest tr = findTransferOrThrow(requestId);
        String me = SystemUtils.getCurrentUsername();
        boolean isRequester = me != null && me.equalsIgnoreCase(tr.getRequesterUsername());
        if (!isRequester && !isAdmin()) {
            throw new QTHTException("error.asset.transfer.cancel.forbidden");
        }
        // Chỉ huỷ khi chưa duyệt xong — kể cả engine mode
        if (!TR_PENDING.equals(tr.getStatus())) {
            throw new QTHTException("error.asset.transfer.cannot.cancel");
        }

        if (tr.getWorkflowInstanceId() != null) {
            try {
                workflowService.cancelInstance(tr.getWorkflowInstanceId());
            } catch (Exception ex) {
                log.warn("[asset] Không cancel được workflow instance {}: {}",
                        tr.getWorkflowInstanceId(), ex.getMessage());
            }
        }
        tr.setStatus(TR_CANCELLED);
        tr.setCancelledAt(LocalDateTime.now());
        transferRequestRepository.save(tr);
        return enrichTransfer(tr, null);
    }

    @Override
    @Transactional
    public AssetTransferRequestResponse handoverTransferRequest(String requestId, String note) {
        AssetTransferRequest tr = findTransferOrThrow(requestId);

        // Ở engine mode, "handover" thực chất chỉ là duyệt STEP CUỐI trong workflow.
        // FE có thể vẫn call endpoint /handover cũ cho tương thích — ta redirect
        // vào cùng logic approve. Nếu step cuối là "Bàn giao", engine sẽ COMPLETED
        // và applyHandoverEffects() sẽ chuyển Asset → IN_USE / AVAILABLE.
        if (tr.getWorkflowInstanceId() != null) {
            return approveViaEngine(tr, note);
        }

        // ---- Legacy path ----
        requireApprover();
        if (!TR_APPROVED.equals(tr.getStatus())) {
            throw new QTHTException("error.asset.transfer.not.approved");
        }
        Asset asset = findOrThrow(tr.getAssetId());
        applyHandoverEffects(tr, asset, note);
        tr.setStatus(TR_HANDED_OVER);
        tr.setHandedOverBy(SystemUtils.getCurrentUsername());
        tr.setHandedOverAt(LocalDateTime.now());
        tr.setHandoverNote(note);
        transferRequestRepository.save(tr);
        return enrichTransfer(tr, asset);
    }

    // ============================================================
    // Workflow engine glue
    // ============================================================

    /**
     * Duyệt bước hiện tại của instance:
     * <ul>
     *   <li>Nếu còn bước tiếp theo → instance vẫn RUNNING, request.status stays PENDING
     *       nhưng approvedBy/At/Note ghi lại cho step vừa duyệt (audit).</li>
     *   <li>Nếu là bước cuối → instance COMPLETED → apply handover cho asset →
     *       request.status = HANDED_OVER.</li>
     * </ul>
     * Engine tự check role/permission của user hiện tại nên không cần {@link #requireApprover()}.
     */
    private AssetTransferRequestResponse approveViaEngine(AssetTransferRequest tr, String note) {
        WorkflowTaskDto task = findCurrentPendingTask(tr);
        if (task == null) {
            // Có thể instance đã hoàn tất trước đó nhưng request chưa sync → force sync
            syncFromEngine(tr);
            return enrichTransfer(tr, null);
        }
        workflowService.approveTask(task.getId(), note);

        // Fetch instance mới để biết đã completed chưa
        WorkflowInstanceDto instance = workflowService
                .findInstanceByEntity(WF_ENTITY_TYPE, tr.getId())
                .orElse(null);

        String me = SystemUtils.getCurrentUsername();
        if (instance != null && "COMPLETED".equals(instance.getStatus())) {
            // Bước cuối — thực hiện side-effect cho asset
            Asset asset = findOrThrow(tr.getAssetId());
            applyHandoverEffects(tr, asset, note);
            tr.setStatus(TR_HANDED_OVER);
            tr.setHandedOverBy(me);
            tr.setHandedOverAt(LocalDateTime.now());
            tr.setHandoverNote(note);
            // Cũng ghi field approvedBy nếu chưa có (audit legacy)
            if (tr.getApprovedBy() == null) {
                tr.setApprovedBy(me);
                tr.setApprovedAt(LocalDateTime.now());
                tr.setApproveNote(note);
            }
            transferRequestRepository.save(tr);
            log.info("[asset] Instance {} COMPLETED — asset {} chuyển IN_USE",
                    instance.getId(), asset.getCode());
            return enrichTransfer(tr, asset);
        }

        // Còn bước tiếp — chỉ ghi audit cho lần duyệt đầu (approvedBy chưa có)
        if (tr.getApprovedBy() == null) {
            tr.setApprovedBy(me);
            tr.setApprovedAt(LocalDateTime.now());
            tr.setApproveNote(note);
            // Với flow N > 1 bước, giữ status PENDING để FE hiểu "vẫn còn bước tiếp"
            transferRequestRepository.save(tr);
        }
        return enrichTransfer(tr, null);
    }

    /**
     * Đồng bộ status của request theo state của workflow instance — dùng khi phát hiện
     * mismatch (VD engine đã COMPLETED nhưng request.status vẫn PENDING do lỗi trước đó).
     */
    private void syncFromEngine(AssetTransferRequest tr) {
        if (tr.getWorkflowInstanceId() == null) return;
        WorkflowInstanceDto inst = workflowService
                .findInstanceByEntity(WF_ENTITY_TYPE, tr.getId())
                .orElse(null);
        if (inst == null) return;
        String s = inst.getStatus();
        if ("COMPLETED".equals(s) && !TR_HANDED_OVER.equals(tr.getStatus())) {
            Asset asset = findOrThrow(tr.getAssetId());
            applyHandoverEffects(tr, asset, null);
            tr.setStatus(TR_HANDED_OVER);
            transferRequestRepository.save(tr);
        } else if ("REJECTED".equals(s) && !TR_REJECTED.equals(tr.getStatus())) {
            tr.setStatus(TR_REJECTED);
            transferRequestRepository.save(tr);
        } else if ("CANCELLED".equals(s) && !TR_CANCELLED.equals(tr.getStatus())) {
            tr.setStatus(TR_CANCELLED);
            transferRequestRepository.save(tr);
        }
    }

    /**
     * Lookup task PENDING của instance đang gắn với 1 ticket (entityId = {@code tr.id}).
     */
    private WorkflowTaskDto findCurrentPendingTask(AssetTransferRequest tr) {
        WorkflowInstanceDto inst = workflowService
                .findInstanceByEntity(WF_ENTITY_TYPE, tr.getId())
                .orElse(null);
        if (inst == null || inst.getTasks() == null) return null;
        return inst.getTasks().stream()
                .filter(t -> "PENDING".equals(t.getStatus()))
                .findFirst().orElse(null);
    }

    /**
     * Cập nhật {@link Asset#status}, {@code assignedPersonId} và ghi 1 dòng {@link AssetAssignment}
     * timeline. Được gọi bởi cả legacy path lẫn engine path khi ticket đến bước bàn giao cuối.
     */
    private void applyHandoverEffects(AssetTransferRequest tr, Asset asset, String note) {
        if (TR_TYPE_ASSIGN.equals(tr.getRequestType())) {
            if (!STATUS_AVAILABLE.equals(asset.getStatus())) {
                throw new QTHTException("error.asset.not.available");
            }
            asset.setStatus(STATUS_IN_USE);
            asset.setAssignedPersonId(tr.getPersonId());
            asset.setAssignedAt(LocalDate.now());
            assetRepository.save(asset);
            writeAssignment(asset.getId(), "ASSIGN", tr.getPersonId(), tr.getPersonName(),
                    LocalDate.now(), joinNote(tr.getReason(), note), null);
        } else if (TR_TYPE_RETURN.equals(tr.getRequestType())) {
            if (!STATUS_IN_USE.equals(asset.getStatus())) {
                throw new QTHTException("error.asset.not.in.use");
            }
            String pid = asset.getAssignedPersonId();
            String pn = pid != null ? lookupPersonName(pid) : null;
            asset.setStatus(STATUS_AVAILABLE);
            asset.setAssignedPersonId(null);
            asset.setAssignedAt(null);
            assetRepository.save(asset);
            writeAssignment(asset.getId(), "RETURN", pid, pn,
                    LocalDate.now(), joinNote(tr.getReason(), note), null);
        }
    }

    @Override
    public Map<String, Object> listTransferRequests(String status, String requestType, String assetId,
                                                    String personId, String keyword, int page, int size) {
        Specification<AssetTransferRequest> spec = (root, q, cb) -> {
            List<Predicate> ands = new ArrayList<>();
            ands.add(cb.equal(root.get("isDeleted"), false));
            if (status != null && !status.isBlank())
                ands.add(cb.equal(root.get("status"), status.toUpperCase()));
            if (requestType != null && !requestType.isBlank())
                ands.add(cb.equal(root.get("requestType"), requestType.toUpperCase()));
            if (assetId != null && !assetId.isBlank())
                ands.add(cb.equal(root.get("assetId"), assetId));
            if (personId != null && !personId.isBlank())
                ands.add(cb.equal(root.get("personId"), personId));
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase() + "%";
                ands.add(cb.or(
                        cb.like(cb.lower(root.get("personName")), like),
                        cb.like(cb.lower(root.get("requesterUsername")), like),
                        cb.like(cb.lower(root.get("reason")), like)
                ));
            }
            return cb.and(ands.toArray(new Predicate[0]));
        };
        Page<AssetTransferRequest> pageResult = transferRequestRepository.findAll(spec,
                ServiceHelper.createPageable(page, size, Sort.by(Sort.Direction.DESC, "createdDate")));

        List<AssetTransferRequestResponse> items = pageResult.getContent().stream()
                .map(tr -> enrichTransfer(tr, null)).toList();
        Map<String, Object> resp = new HashMap<>();
        resp.put("pageNumber", page);
        resp.put("pageSize", size);
        resp.put("total", pageResult.getTotalElements());
        resp.put("items", items);
        return resp;
    }

    @Override
    public AssetTransferRequestResponse getTransferRequest(String requestId) {
        return enrichTransfer(findTransferOrThrow(requestId), null);
    }

    // ---- Helpers ----

    private AssetTransferRequest findTransferOrThrow(String id) {
        return transferRequestRepository.findById(id)
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new QTHTException("error.asset.transfer.not.found"));
    }

    /**
     * Placeholder policy: hiện tại chỉ admin approve/handover. Sau khi có org tree +
     * asset manager role → refactor thành strategy (AssetApprovalResolver).
     */
    private void requireApprover() {
        if (!isAdmin()) {
            throw new QTHTException("error.asset.transfer.approve.forbidden");
        }
    }

    /**
     * Kiểm tra admin bằng cách lookup {@code user → person.is_admin} qua JDBC.
     * <p>JWT filter chỉ set username vào SecurityContext, không có claim isAdmin →
     * đây là single source of truth (Person.isAdmin).
     * <p>Admin bootstrap {@code admin} / {@code superadmin} luôn được coi là admin
     * kể cả khi Person chưa link (fallback an toàn cho local dev + first-time seed).
     */
    private boolean isAdmin() {
        String username = SystemUtils.getCurrentUsername();
        if (username == null || username.isBlank()) return false;
        String lower = username.toLowerCase();
        if ("admin".equals(lower) || "superadmin".equals(lower)) return true;
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users u JOIN person p ON p.id = u.person_id " +
                            "WHERE u.user_name = ? AND p.is_admin = true " +
                            "AND (u.is_deleted IS NULL OR u.is_deleted = false) " +
                            "AND (p.is_deleted IS NULL OR p.is_deleted = false)",
                    Integer.class, username);
            return count != null && count > 0;
        } catch (Exception ex) {
            log.warn("[asset] Không check được admin cho '{}': {}", username, ex.getMessage());
            return false;
        }
    }

    private static String joinNote(String reason, String handoverNote) {
        StringBuilder sb = new StringBuilder();
        if (reason != null && !reason.isBlank()) sb.append("Lý do: ").append(reason.trim());
        if (handoverNote != null && !handoverNote.isBlank()) {
            if (sb.length() > 0) sb.append("\nBàn giao: ").append(handoverNote.trim());
            else sb.append(handoverNote.trim());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private AssetTransferRequestResponse enrichTransfer(AssetTransferRequest tr, Asset preloaded) {
        AssetTransferRequestResponse r = new AssetTransferRequestResponse();
        r.setId(tr.getId());
        r.setAssetId(tr.getAssetId());
        r.setRequestType(tr.getRequestType());
        r.setStatus(tr.getStatus());
        r.setRequesterUsername(tr.getRequesterUsername());
        r.setPersonId(tr.getPersonId());
        r.setPersonName(tr.getPersonName());
        r.setReason(tr.getReason());
        r.setPlannedDate(tr.getPlannedDate());
        r.setApprovedBy(tr.getApprovedBy());
        r.setApprovedAt(tr.getApprovedAt() != null ? tr.getApprovedAt().toString() : null);
        r.setApproveNote(tr.getApproveNote());
        r.setRejectedBy(tr.getRejectedBy());
        r.setRejectedAt(tr.getRejectedAt() != null ? tr.getRejectedAt().toString() : null);
        r.setRejectReason(tr.getRejectReason());
        r.setCancelledAt(tr.getCancelledAt() != null ? tr.getCancelledAt().toString() : null);
        r.setHandedOverBy(tr.getHandedOverBy());
        r.setHandedOverAt(tr.getHandedOverAt() != null ? tr.getHandedOverAt().toString() : null);
        r.setHandoverNote(tr.getHandoverNote());
        r.setCreatedBy(tr.getCreatedBy());
        r.setCreatedDate(tr.getCreatedDate() != null ? tr.getCreatedDate().toString() : null);

        // Enrich asset code/name để hiển thị trên FE (khỏi phải fetch lần 2)
        Asset asset = preloaded;
        if (asset == null) {
            asset = assetRepository.findById(tr.getAssetId()).orElse(null);
        }
        if (asset != null) {
            r.setAssetCode(asset.getCode());
            r.setAssetName(asset.getName());
        }

        // ---- Workflow enrichment ----
        // Trả kèm currentTaskId + stepName để FE có thể render nút "Duyệt: <stepName>"
        // và query GET /wf/instances/by-entity/ASSET_TRANSFER/{id} nếu muốn stepper full.
        r.setWorkflowInstanceId(tr.getWorkflowInstanceId());
        if (tr.getWorkflowInstanceId() != null) {
            r.setWorkflowEntityType(WF_ENTITY_TYPE);
            try {
                WorkflowInstanceDto inst = workflowService
                        .findInstanceByEntity(WF_ENTITY_TYPE, tr.getId())
                        .orElse(null);
                if (inst != null && inst.getTasks() != null) {
                    inst.getTasks().stream()
                            .filter(t -> "PENDING".equals(t.getStatus()))
                            .findFirst()
                            .ifPresent(t -> {
                                r.setCurrentTaskId(t.getId());
                                r.setCurrentStepName(t.getStepName());
                            });
                }
            } catch (Exception ex) {
                log.debug("[asset] Không enrich được workflow state cho request {}: {}",
                        tr.getId(), ex.getMessage());
            }
        }
        return r;
    }
}
