package com.frezo.task.service.impl;

import com.frezo.auth.repository.UserRepository;
import com.frezo.common.exception.AppException;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.repository.CommentAttachmentRepository;
import com.frezo.common.repository.CommentRepository;
import com.frezo.common.service.NotificationService;
import com.frezo.task.common.TaskErrorCode;
import com.frezo.task.dto.request.ReviewRequest;
import com.frezo.task.dto.request.TicketRequest;
import com.frezo.task.dto.response.TicketResponse;
import com.frezo.task.entity.Ticket;
import com.frezo.task.entity.TicketCategory;
import com.frezo.task.mapper.TicketMapper;
import com.frezo.task.repository.TicketCategoryRepository;
import com.frezo.task.repository.TicketRepository;
import com.frezo.task.security.TaskAccessHelper;
import com.frezo.task.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Ticket service — vòng đời OPEN → IN_PROGRESS → RESOLVED (chờ QL duyệt) → CLOSED.
 *
 * <p>Visibility: admin = tất cả; reporter/createdBy = ticket đã giao; assignee = ticket của mình.
 * <p>RESOLVED = EU hoàn thành, chờ người giao check; CLOSED = đã duyệt.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final String TICKET_ENTITY = "TICKET";
    private static final String SUBJECT_TYPE_TICKET = "TICKET";
    private static final String ACTION_URL_PREFIX = "/task?tab=board&ticketId=";

    private final TicketRepository ticketRepository;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final TicketMapper ticketMapper;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final CommentAttachmentRepository commentAttachmentRepository;
    private final TaskAccessHelper accessHelper;

    // ============================================================
    // CRUD
    // ============================================================

    @Override
    @Transactional
    public TicketResponse create(TicketRequest request) {
        validateCategoryCode(request.getCategory());
        Ticket ticket = ticketMapper.toEntity(request);
        String currentUser = SystemUtils.getCurrentUsername();
        ticket.setReporterId(currentUser);
        ticket.setCode("TICKET-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());

        if (ticket.getStatus() == null) {
            ticket.setStatus(Ticket.TicketStatus.OPEN);
        }
        // Chỉ admin mới được tạo thẳng CLOSED; EU không tự đóng
        if (ticket.getStatus() == Ticket.TicketStatus.CLOSED && !accessHelper.isAdmin()) {
            ticket.setStatus(Ticket.TicketStatus.OPEN);
        }
        if (ticket.getStatus() == Ticket.TicketStatus.RESOLVED && !accessHelper.isAdmin()) {
            // create + resolve ngay: vẫn OK nếu tự assign mình; giữ RESOLVED
            if (ticket.getAssigneeId() == null) {
                accessHelper.currentPersonId().ifPresent(ticket::setAssigneeId);
            }
        }

        Ticket saved = ticketRepository.save(ticket);

        if (saved.getAssigneeId() != null) {
            notifyAssignment(saved, /*prevAssignee*/ null, currentUser);
        }

        return toVisibleResponse(saved);
    }

    @Override
    @Transactional
    public TicketResponse update(String id, TicketRequest request) {
        Ticket ticket = requireVisibleTicket(id);

        if (request.getCategory() != null) {
            validateCategoryCode(request.getCategory());
        }

        Ticket.TicketStatus oldStatus = ticket.getStatus();
        String oldAssigneeId = ticket.getAssigneeId();

        ticketMapper.updateEntityFromRequest(request, ticket);

        if (request.getAssigneeId() != null && request.getAssigneeId().isBlank()) {
            ticket.setAssigneeId(null);
        }

        if (request.getStatus() != null && oldStatus != ticket.getStatus()) {
            enforceStatusTransition(ticket, oldStatus, ticket.getStatus());
        }

        if (ticket.getStatus() == Ticket.TicketStatus.RESOLVED || ticket.getStatus() == Ticket.TicketStatus.CLOSED) {
            if (ticket.getResolvedAt() == null) {
                ticket.setResolvedAt(LocalDateTime.now());
            }
        }

        Ticket saved = ticketRepository.save(ticket);
        String currentUser = SystemUtils.getCurrentUsername();

        if (!Objects.equals(oldAssigneeId, saved.getAssigneeId())) {
            notifyAssignment(saved, oldAssigneeId, currentUser);
        }
        if (oldStatus != saved.getStatus()) {
            notifyStatusChange(saved, oldStatus, currentUser);
        }

        return toVisibleResponse(saved);
    }

    @Override
    @Transactional
    public void delete(String id) {
        Ticket ticket = requireVisibleTicket(id);
        // Chỉ reporter / admin xóa
        if (!accessHelper.canReviewTicket(ticket)) {
            throw new AppException(TaskErrorCode.TICKET_ACCESS_DENIED);
        }
        ticketRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse findById(String id) {
        return toVisibleResponse(requireVisibleTicket(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findAll() {
        List<Ticket> tickets = ticketRepository.findAll().stream()
                .filter(accessHelper::canViewTicket)
                .toList();
        List<TicketResponse> responses = ticketMapper.toResponseList(tickets);
        enrichCountsBatch(responses);
        for (int i = 0; i < responses.size(); i++) {
            enrichAccessFlags(responses.get(i), tickets.get(i));
        }
        return responses;
    }

    // ============================================================
    // Status + Assignment + Review
    // ============================================================

    @Override
    @Transactional
    public TicketResponse updateStatus(String id, String status) {
        Ticket ticket = requireVisibleTicket(id);
        Ticket.TicketStatus oldStatus = ticket.getStatus();
        Ticket.TicketStatus newStatus;
        try {
            newStatus = Ticket.TicketStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(TaskErrorCode.TICKET_STATUS_INVALID);
        }

        enforceStatusTransition(ticket, oldStatus, newStatus);
        ticket.setStatus(newStatus);
        if (newStatus == Ticket.TicketStatus.RESOLVED || newStatus == Ticket.TicketStatus.CLOSED) {
            if (ticket.getResolvedAt() == null) {
                ticket.setResolvedAt(LocalDateTime.now());
            }
        }
        Ticket saved = ticketRepository.save(ticket);

        if (oldStatus != saved.getStatus()) {
            notifyStatusChange(saved, oldStatus, SystemUtils.getCurrentUsername());
        }
        return toVisibleResponse(saved);
    }

    @Override
    @Transactional
    public TicketResponse assignTicket(String id, String assigneeId) {
        Ticket ticket = requireVisibleTicket(id);
        if (!accessHelper.canReviewTicket(ticket) && !accessHelper.isAdmin()) {
            throw new AppException(TaskErrorCode.TICKET_ACCESS_DENIED);
        }

        String oldAssigneeId = ticket.getAssigneeId();
        ticket.setAssigneeId(assigneeId);
        if (ticket.getStatus() == Ticket.TicketStatus.OPEN) {
            ticket.setStatus(Ticket.TicketStatus.IN_PROGRESS);
        }
        Ticket saved = ticketRepository.save(ticket);

        if (!Objects.equals(oldAssigneeId, saved.getAssigneeId())) {
            notifyAssignment(saved, oldAssigneeId, SystemUtils.getCurrentUsername());
        }
        return toVisibleResponse(saved);
    }

    @Override
    @Transactional
    public TicketResponse review(String id, ReviewRequest request) {
        Ticket ticket = requireVisibleTicket(id);
        if (!accessHelper.canReviewTicket(ticket)) {
            throw new AppException(TaskErrorCode.TICKET_REVIEW_FORBIDDEN);
        }
        if (ticket.getStatus() != Ticket.TicketStatus.RESOLVED) {
            throw new AppException(TaskErrorCode.TICKET_REVIEW_INVALID);
        }

        Ticket.TicketStatus oldStatus = ticket.getStatus();
        boolean approved = request != null && request.isApproved();
        if (approved) {
            ticket.setStatus(Ticket.TicketStatus.CLOSED);
            if (ticket.getResolvedAt() == null) {
                ticket.setResolvedAt(LocalDateTime.now());
            }
            if (request.getNote() != null && !request.getNote().isBlank()) {
                String existing = ticket.getResolutionNote();
                String reviewNote = "QL duyệt: " + request.getNote().trim();
                ticket.setResolutionNote(existing == null || existing.isBlank()
                        ? reviewNote
                        : existing + "\n" + reviewNote);
            }
        } else {
            ticket.setStatus(Ticket.TicketStatus.IN_PROGRESS);
            ticket.setResolvedAt(null);
            if (request != null && request.getNote() != null && !request.getNote().isBlank()) {
                String existing = ticket.getResolutionNote();
                String rejectNote = "QL trả lại: " + request.getNote().trim();
                ticket.setResolutionNote(existing == null || existing.isBlank()
                        ? rejectNote
                        : existing + "\n" + rejectNote);
            }
        }

        Ticket saved = ticketRepository.save(ticket);
        String actor = SystemUtils.getCurrentUsername();
        notifyReviewResult(saved, approved, actor);
        if (oldStatus != saved.getStatus()) {
            notifyStatusChange(saved, oldStatus, actor);
        }
        return toVisibleResponse(saved);
    }

    /**
     * RESOLVED chỉ assignee/admin; CLOSED chỉ reporter/admin (qua review hoặc admin).
     * Reject review dùng RESOLVED → IN_PROGRESS (reporter).
     */
    private void enforceStatusTransition(Ticket ticket, Ticket.TicketStatus from, Ticket.TicketStatus to) {
        if (from == to) return;
        if (accessHelper.isAdmin()) return;

        if (to == Ticket.TicketStatus.RESOLVED) {
            if (!accessHelper.canCompleteTicket(ticket)) {
                throw new AppException(TaskErrorCode.TICKET_COMPLETE_FORBIDDEN);
            }
            return;
        }
        if (to == Ticket.TicketStatus.CLOSED) {
            // Prefer dedicated /review; allow status patch only for reviewer
            if (!accessHelper.canReviewTicket(ticket)) {
                throw new AppException(TaskErrorCode.TICKET_REVIEW_FORBIDDEN);
            }
            if (from != Ticket.TicketStatus.RESOLVED) {
                throw new AppException(TaskErrorCode.TICKET_REVIEW_INVALID);
            }
            return;
        }
        // RESOLVED → IN_PROGRESS = reject by reviewer
        if (from == Ticket.TicketStatus.RESOLVED && to == Ticket.TicketStatus.IN_PROGRESS) {
            if (!accessHelper.canReviewTicket(ticket)) {
                throw new AppException(TaskErrorCode.TICKET_REVIEW_FORBIDDEN);
            }
            return;
        }
        // CLOSED không tự mở lại trừ admin (đã return sớm)
        if (from == Ticket.TicketStatus.CLOSED) {
            throw new AppException(TaskErrorCode.TICKET_REVIEW_FORBIDDEN);
        }
    }

    private Ticket requireVisibleTicket(String id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new AppException(TaskErrorCode.TICKET_NOT_FOUND));
        if (!accessHelper.canViewTicket(ticket)) {
            throw new AppException(TaskErrorCode.TICKET_ACCESS_DENIED);
        }
        return ticket;
    }

    private TicketResponse toVisibleResponse(Ticket ticket) {
        TicketResponse response = enrichCounts(ticketMapper.toResponse(ticket));
        enrichAccessFlags(response, ticket);
        return response;
    }

    private void enrichAccessFlags(TicketResponse response, Ticket ticket) {
        if (response == null || ticket == null) return;
        boolean pending = ticket.getStatus() == Ticket.TicketStatus.RESOLVED;
        response.setPendingReview(pending);
        response.setCanReview(pending && accessHelper.canReviewTicket(ticket));
        response.setCanComplete(accessHelper.canCompleteTicket(ticket));
    }

    // ============================================================
    // Comment / attachment aggregates
    // ============================================================

    private TicketResponse enrichCounts(TicketResponse response) {
        if (response == null || response.getId() == null) return response;
        long comments = commentRepository.countUserComments(SUBJECT_TYPE_TICKET, response.getId());
        response.setCommentCount((int) comments);
        Map<String, Integer> att = attachmentCountsFor(List.of(response.getId()));
        response.setAttachmentCount(att.getOrDefault(response.getId(), 0));
        enrichCategoryName(response, loadCategoryNameMap());
        return response;
    }

    private void enrichCountsBatch(List<TicketResponse> responses) {
        if (responses == null || responses.isEmpty()) return;
        List<String> ids = responses.stream()
                .map(TicketResponse::getId)
                .filter(Objects::nonNull)
                .toList();
        if (ids.isEmpty()) return;

        Map<String, Integer> commentCounts = new HashMap<>();
        for (Object[] row : commentRepository.countUserCommentsGrouped(SUBJECT_TYPE_TICKET, ids)) {
            if (row == null || row.length < 2 || row[0] == null) continue;
            commentCounts.put(String.valueOf(row[0]), ((Number) row[1]).intValue());
        }
        Map<String, Integer> attachmentCounts = attachmentCountsFor(ids);
        Map<String, String> categoryNames = loadCategoryNameMap();

        for (TicketResponse r : responses) {
            r.setCommentCount(commentCounts.getOrDefault(r.getId(), 0));
            r.setAttachmentCount(attachmentCounts.getOrDefault(r.getId(), 0));
            enrichCategoryName(r, categoryNames);
        }
    }

    private void validateCategoryCode(String code) {
        if (code == null || code.isBlank()) {
            return;
        }
        ticketCategoryRepository.findByCodeAndIsDeletedFalse(code.trim())
                .orElseThrow(() -> new AppException(TaskErrorCode.TICKET_CATEGORY_INVALID, code));
    }

    private Map<String, String> loadCategoryNameMap() {
        Map<String, String> map = new HashMap<>();
        for (TicketCategory c : ticketCategoryRepository.findByIsDeletedFalseOrderBySortOrderAscNameAsc()) {
            if (c.getCode() != null) {
                map.put(c.getCode(), c.getName());
            }
        }
        return map;
    }

    private void enrichCategoryName(TicketResponse response, Map<String, String> categoryNames) {
        if (response == null || response.getCategory() == null) return;
        String name = categoryNames.get(response.getCategory());
        if (name != null) {
            response.setCategoryName(name);
        }
    }

    private Map<String, Integer> attachmentCountsFor(List<String> ticketIds) {
        Map<String, Integer> out = new HashMap<>();
        if (ticketIds == null || ticketIds.isEmpty()) return out;
        for (Object[] row : commentAttachmentRepository.countBySubjectIds(SUBJECT_TYPE_TICKET, ticketIds)) {
            if (row == null || row.length < 2 || row[0] == null) continue;
            out.put(String.valueOf(row[0]), ((Number) row[1]).intValue());
        }
        return out;
    }

    // ============================================================
    // Notifications
    // ============================================================

    private void notifyAssignment(Ticket ticket, String prevAssigneeId, String actor) {
        String actionUrl = ACTION_URL_PREFIX + ticket.getId();
        String ticketLabel = "#" + safe(ticket.getCode()) + " – " + safe(ticket.getTitle());

        String newAssigneeUsername = resolveUsername(ticket.getAssigneeId());
        if (newAssigneeUsername != null) {
            notificationService.notify(
                    newAssigneeUsername,
                    "Bạn có ticket mới cần xử lý",
                    "Ticket " + ticketLabel + " vừa được giao cho bạn.",
                    "TICKET_ASSIGNED",
                    TICKET_ENTITY,
                    ticket.getId(),
                    actionUrl,
                    actor,
                    isUrgent(ticket)
            );
        }

        if (prevAssigneeId != null && !Objects.equals(prevAssigneeId, ticket.getAssigneeId())) {
            String oldAssigneeUsername = resolveUsername(prevAssigneeId);
            if (oldAssigneeUsername != null && !Objects.equals(oldAssigneeUsername, actor)) {
                notificationService.notify(
                        oldAssigneeUsername,
                        "Ticket đã chuyển sang người khác",
                        "Ticket " + ticketLabel + " không còn thuộc về bạn.",
                        "TICKET_UNASSIGNED",
                        TICKET_ENTITY,
                        ticket.getId(),
                        actionUrl,
                        actor,
                        false
                );
            }
        }

        if (ticket.getReporterId() != null && !Objects.equals(ticket.getReporterId(), actor)) {
            notificationService.notify(
                    ticket.getReporterId(),
                    "Ticket của bạn đã được giao",
                    "Ticket " + ticketLabel + " đã có người xử lý.",
                    "TICKET_ASSIGNED_TO_OTHER",
                    TICKET_ENTITY,
                    ticket.getId(),
                    actionUrl,
                    actor,
                    false
            );
        }
    }

    private void notifyStatusChange(Ticket ticket, Ticket.TicketStatus oldStatus, String actor) {
        String actionUrl = ACTION_URL_PREFIX + ticket.getId();
        String ticketLabel = "#" + safe(ticket.getCode()) + " – " + safe(ticket.getTitle());
        String statusText = statusLabel(ticket.getStatus());
        boolean urgent = ticket.getStatus() == Ticket.TicketStatus.RESOLVED
                || ticket.getStatus() == Ticket.TicketStatus.CLOSED
                || isUrgent(ticket);

        List<String> recipients = new ArrayList<>();
        if (ticket.getReporterId() != null && !Objects.equals(ticket.getReporterId(), actor)) {
            recipients.add(ticket.getReporterId());
        }
        String assigneeUsername = resolveUsername(ticket.getAssigneeId());
        if (assigneeUsername != null && !Objects.equals(assigneeUsername, actor)) {
            recipients.add(assigneeUsername);
        }

        String notifType = ticket.getStatus() == Ticket.TicketStatus.RESOLVED
                ? "TICKET_PENDING_REVIEW"
                : "TICKET_STATUS_CHANGED";
        String title = ticket.getStatus() == Ticket.TicketStatus.RESOLVED
                ? "Ticket chờ bạn duyệt hoàn thành"
                : "Ticket đổi trạng thái: " + statusText;

        notificationService.notifyMany(
                recipients,
                title,
                "Ticket " + ticketLabel + " chuyển từ "
                        + statusLabel(oldStatus) + " → " + statusText + ".",
                notifType,
                TICKET_ENTITY,
                ticket.getId(),
                actionUrl,
                actor,
                urgent
        );
    }

    private void notifyReviewResult(Ticket ticket, boolean approved, String actor) {
        String actionUrl = ACTION_URL_PREFIX + ticket.getId();
        String ticketLabel = "#" + safe(ticket.getCode()) + " – " + safe(ticket.getTitle());
        String assigneeUsername = resolveUsername(ticket.getAssigneeId());
        if (assigneeUsername == null || Objects.equals(assigneeUsername, actor)) return;

        notificationService.notify(
                assigneeUsername,
                approved ? "Quản lý đã duyệt ticket hoàn thành" : "Ticket bị trả lại — cần xử lý tiếp",
                "Ticket " + ticketLabel + (approved
                        ? " đã được đóng."
                        : " bị trả về Đang xử lý."),
                approved ? "TICKET_REVIEW_APPROVED" : "TICKET_REVIEW_REJECTED",
                TICKET_ENTITY,
                ticket.getId(),
                actionUrl,
                actor,
                !approved
        );
    }

    private String resolveUsername(String personId) {
        if (personId == null || personId.isBlank()) return null;
        return userRepository.findByPersonId(personId)
                .map(u -> u.getUserName())
                .or(() -> userRepository.findByUserName(personId).map(u -> u.getUserName()))
                .orElse(null);
    }

    private static String statusLabel(Ticket.TicketStatus s) {
        if (s == null) return "—";
        return switch (s) {
            case OPEN -> "Mới";
            case IN_PROGRESS -> "Đang xử lý";
            case RESOLVED -> "Chờ quản lý duyệt";
            case CLOSED -> "Đã đóng";
        };
    }

    private static boolean isUrgent(Ticket t) {
        return t.getPriority() == Ticket.TicketPriority.URGENT
                || t.getPriority() == Ticket.TicketPriority.HIGH;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
