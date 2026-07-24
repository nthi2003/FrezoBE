package com.frezo.qlns.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.service.NotificationService;
import com.frezo.qlns.dto.request.LeaveRequestAddRequest;
import com.frezo.qlns.dto.response.LeaveRequestHistoryResponse;
import com.frezo.qlns.dto.response.LeaveRequestResponse;
import com.frezo.qlns.entity.LeaveRequest;
import com.frezo.qlns.entity.LeaveRequestHistory;
import com.frezo.qlns.mapper.LeaveRequestMapper;
import com.frezo.qlns.repository.LeaveRequestHistoryRepository;
import com.frezo.qlns.repository.LeaveRequestRepository;
import com.frezo.qlns.service.LeaveRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.frezo.qlns.service.impl.LeaveApprovalBridge;

import java.util.List;
import java.util.Map;

/**
 * Workflow nghỉ phép — tạo đơn gắn Approval LEAVE_STANDARD.
 * Duyệt/từ chối qua {@code /approvals/*} (approve|reject cũ trả 410).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveRequestServiceImpl implements LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveRequestHistoryRepository historyRepository;
    private final LeaveRequestMapper leaveRequestMapper;
    private final NotificationService notificationService;
    private final LeaveApprovalBridge approvalBridge;

    private static final String STATUS_PENDING_MANAGER = "PENDING_MANAGER";
    private static final String STATUS_PENDING_HR = "PENDING_HR";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_LEGACY_PENDING = "PENDING";

    @Override
    @Transactional
    public LeaveRequestResponse create(LeaveRequestAddRequest request) {

        approvalBridge.assertActiveContract(request.getContractId());

        LeaveRequest entity = leaveRequestMapper.toEntity(request);

        String managerUsername = approvalBridge.resolveManagerUsername(request.getPersonId());
        entity.setManagerUsername(managerUsername);
        entity.setStatus(STATUS_PENDING_MANAGER);

        LeaveRequest saved = leaveRequestRepository.save(entity);

        String requester = SystemUtils.getCurrentUsername();
        writeHistory(saved.getId(), "SUBMIT", null, saved.getStatus(), requester, "REQUESTER",
                "Tạo đơn + Approval LEAVE_STANDARD");

        approvalBridge.start(saved);
        notifyManagerPending(saved);

        return enrich(saved);
    }

    @Override
    @Transactional
    public LeaveRequestResponse approve(String id) {
        throw new AppException(CommonErrorCode.GONE,
                "API duyệt leave đã ngừng — dùng POST /approvals/{id}/approve");
    }

    @Override
    @Transactional
    public LeaveRequestResponse reject(String id, String reason) {
        throw new AppException(CommonErrorCode.GONE,
                "API từ chối leave đã ngừng — dùng POST /approvals/{id}/reject");
    }

    @Override
    @Transactional
    public LeaveRequestResponse cancel(String id) {
        LeaveRequest entity = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new QTHTException("error.leave.request.not.found"));

        String currentStatus = normalizeStatus(entity.getStatus());
        if (!STATUS_PENDING_MANAGER.equals(currentStatus) && !STATUS_PENDING_HR.equals(currentStatus)) {
            throw new QTHTException("error.leave.request.invalid.status");
        }
        String actor = SystemUtils.getCurrentUsername();
        if (!approvalBridge.isCurrentUserAdmin() && !actor.equals(entity.getCreatedBy())) {
            throw new QTHTException("error.leave.request.permission.denied");
        }

        entity.setStatus(STATUS_CANCELLED);
        LeaveRequest saved = leaveRequestRepository.save(entity);

        writeHistory(id, "CANCEL", currentStatus, STATUS_CANCELLED, actor, "REQUESTER", null);

        String recipient = STATUS_PENDING_MANAGER.equals(currentStatus)
                ? entity.getManagerUsername()
                : (approvalBridge.resolveHrUsernames().isEmpty() ? null : approvalBridge.resolveHrUsernames().get(0));
        if (recipient != null) {
            notificationService.notify(recipient,
                    "Đơn nghỉ phép bị huỷ",
                    "Nhân viên " + entity.getCreatedBy() + " đã huỷ đơn nghỉ phép.",
                    "LEAVE_CANCELLED", "LEAVE", id,
                    "/qlns/leaves?highlight=" + id, actor, false);
        }
        return enrich(saved);
    }

    @Override
    public List<LeaveRequestResponse> getMyRequests(String contractId) {
        List<LeaveRequest> all = leaveRequestRepository.findByContractIdOrderByCreatedDateDesc(contractId);
        return all.stream().map(this::enrich).toList();
    }

    @Override
    public Map<String, Object> allPending(int page, int size) {
        String currentUser = SystemUtils.getCurrentUsername();
        boolean isAdmin = approvalBridge.isCurrentUserAdmin();
        boolean isHr = approvalBridge.resolveHrUsernames().contains(currentUser);

        Specification<LeaveRequest> spec;
        if (isAdmin) {
            spec = (root, q, cb) -> cb.or(
                    cb.equal(root.get("status"), STATUS_PENDING_MANAGER),
                    cb.equal(root.get("status"), STATUS_PENDING_HR),
                    cb.equal(root.get("status"), STATUS_LEGACY_PENDING));
        } else if (isHr) {
            spec = (root, q, cb) -> cb.equal(root.get("status"), STATUS_PENDING_HR);
        } else {
            spec = (root, q, cb) -> cb.and(
                    cb.or(
                            cb.equal(root.get("status"), STATUS_PENDING_MANAGER),
                            cb.equal(root.get("status"), STATUS_LEGACY_PENDING)),
                    cb.equal(root.get("managerUsername"), currentUser));
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate");
        Page<LeaveRequest> pagedResult = leaveRequestRepository.findAll(spec, ServiceHelper.createPageable(page, size, sort));

        List<LeaveRequestResponse> responses = pagedResult.getContent().stream().map(this::enrich).toList();
        return Map.of(
                "pageNumber", page,
                "pageSize", size,
                "total", pagedResult.getTotalElements(),
                "items", responses);
    }

    @Override
    public List<LeaveRequestHistoryResponse> getHistory(String requestId) {
        return historyRepository.findByRequestIdOrderByCreatedDateAsc(requestId).stream()
                .map(h -> {
                    LeaveRequestHistoryResponse r = new LeaveRequestHistoryResponse();
                    r.setId(h.getId());
                    r.setAction(h.getAction());
                    r.setFromStatus(h.getFromStatus());
                    r.setToStatus(h.getToStatus());
                    r.setActorUsername(h.getActorUsername());
                    r.setActorRole(h.getActorRole());
                    r.setComment(h.getComment());
                    r.setCreatedDate(h.getCreatedDate() != null ? h.getCreatedDate().toString() : null);
                    return r;
                })
                .toList();
    }

    private String normalizeStatus(String raw) {
        return STATUS_LEGACY_PENDING.equals(raw) ? STATUS_PENDING_MANAGER : raw;
    }

    private void writeHistory(String requestId, String action, String from, String to,
                              String actor, String role, String comment) {
        try {
            historyRepository.save(LeaveRequestHistory.builder()
                    .requestId(requestId)
                    .action(action)
                    .fromStatus(from)
                    .toStatus(to)
                    .actorUsername(actor)
                    .actorRole(role)
                    .comment(comment)
                    .build());
        } catch (Exception e) {
            log.error("Không ghi được leave request history: {}", e.getMessage());
        }
    }

    private String buildDateRange(LeaveRequest e) {
        if (e.getStartDate() == null) return "";
        if (e.getEndDate() == null || e.getStartDate().equals(e.getEndDate())) return e.getStartDate().toString();
        return e.getStartDate() + " → " + e.getEndDate();
    }

    private String deepLink(String id) {
        return "/qlns/leaves?highlight=" + id;
    }

    private void notifyManagerPending(LeaveRequest e) {
        if (e.getManagerUsername() == null) return;
        String range = buildDateRange(e);
        notificationService.notify(
                e.getManagerUsername(),
                "Đơn nghỉ phép mới cần duyệt",
                "Nhân viên " + e.getCreatedBy() + " xin nghỉ " + range
                        + (e.getReason() != null ? " · Lý do: " + trunc(e.getReason(), 100) : ""),
                "LEAVE_PENDING_MANAGER", "LEAVE", e.getId(),
                deepLink(e.getId()), e.getCreatedBy(), false);
    }

    private String trunc(String s, int n) {
        if (s == null) return "";
        return s.length() > n ? s.substring(0, n) + "..." : s;
    }

    private LeaveRequestResponse enrich(LeaveRequest e) {
        LeaveRequestResponse r = leaveRequestMapper.toResponse(e);
        r.setManagerUsername(e.getManagerUsername());
        r.setManagerApprovedBy(e.getManagerApprovedBy());
        r.setManagerApprovedAt(e.getManagerApprovedAt());
        r.setHrApprovedBy(e.getHrApprovedBy());
        r.setHrApprovedAt(e.getHrApprovedAt());
        r.setAttachmentUrl(e.getAttachmentUrl());
        r.setRejectedAt(e.getRejectedAt());
        r.setApprovalRequestId(e.getApprovalRequestId());
        return r;
    }
}
