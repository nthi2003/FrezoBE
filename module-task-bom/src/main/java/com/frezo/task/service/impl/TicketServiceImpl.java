package com.frezo.task.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.task.common.TaskErrorCode;
import com.frezo.auth.repository.UserRepository;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.repository.CommentAttachmentRepository;
import com.frezo.common.repository.CommentRepository;
import com.frezo.common.service.NotificationService;
import com.frezo.task.dto.request.TicketRequest;
import com.frezo.task.dto.response.TicketResponse;
import com.frezo.task.entity.Ticket;
import com.frezo.task.entity.TicketCategory;
import com.frezo.task.mapper.TicketMapper;
import com.frezo.task.repository.TicketCategoryRepository;
import com.frezo.task.repository.TicketRepository;
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
 * Ticket service — vòng đời OPEN → IN_PROGRESS → RESOLVED → CLOSED.
 *
 * <p><b>Notification wiring (v1.2):</b> Mọi thay đổi trạng thái hoặc assignee đều
 * emit notification realtime tới các bên liên quan:
 * <ul>
 *   <li><b>Người giao (reporter)</b> — luôn được thông báo khi ticket đổi trạng thái, để nắm tình hình.</li>
 *   <li><b>Người xử lý (assignee)</b> — được thông báo khi ticket được giao cho họ, hoặc khi có action ngoài họ.</li>
 * </ul>
 * Notification bao gồm deep-link {@code /task/tickets?ticketId=...} để FE navigate ngay khi click.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final String TICKET_ENTITY = "TICKET";
    private static final String SUBJECT_TYPE_TICKET = "TICKET";
    private static final String ACTION_URL_PREFIX = "/task/tickets?ticketId=";

    private final TicketRepository ticketRepository;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final TicketMapper ticketMapper;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final CommentAttachmentRepository commentAttachmentRepository;

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

        Ticket saved = ticketRepository.save(ticket);

        // Notify assignee nếu có assign ngay lúc tạo
        if (saved.getAssigneeId() != null) {
            notifyAssignment(saved, /*prevAssignee*/ null, currentUser);
        }

        return enrichCounts(ticketMapper.toResponse(saved));
    }

    @Override
    @Transactional
    public TicketResponse update(String id, TicketRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new AppException(TaskErrorCode.TICKET_NOT_FOUND));

        if (request.getCategory() != null) {
            validateCategoryCode(request.getCategory());
        }

        // Snapshot trước khi update để so sánh
        Ticket.TicketStatus oldStatus = ticket.getStatus();
        String oldAssigneeId = ticket.getAssigneeId();

        // Partial update: null trong request KHÔNG ghi đè (mapper IGNORE).
        ticketMapper.updateEntityFromRequest(request, ticket);

        // intentional clear: client gửi "" / blank → unassign (khác với omit/null = giữ nguyên)
        if (request.getAssigneeId() != null && request.getAssigneeId().isBlank()) {
            ticket.setAssigneeId(null);
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

        return enrichCounts(ticketMapper.toResponse(saved));
    }

    @Override
    @Transactional
    public void delete(String id) {
        if (!ticketRepository.existsById(id)) {
            throw new AppException(TaskErrorCode.TICKET_NOT_FOUND);
        }
        ticketRepository.deleteById(id);
    }

    @Override
    public TicketResponse findById(String id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new AppException(TaskErrorCode.TICKET_NOT_FOUND));
        return enrichCounts(ticketMapper.toResponse(ticket));
    }

    @Override
    public List<TicketResponse> findAll() {
        List<Ticket> tickets = ticketRepository.findAll();
        List<TicketResponse> responses = ticketMapper.toResponseList(tickets);
        enrichCountsBatch(responses);
        return responses;
    }

    // ============================================================
    // Status + Assignment (dedicated endpoints)
    // ============================================================

    @Override
    @Transactional
    public TicketResponse updateStatus(String id, String status) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new AppException(TaskErrorCode.TICKET_NOT_FOUND));

        Ticket.TicketStatus oldStatus = ticket.getStatus();
        try {
            ticket.setStatus(Ticket.TicketStatus.valueOf(status.toUpperCase()));
            if (ticket.getStatus() == Ticket.TicketStatus.RESOLVED || ticket.getStatus() == Ticket.TicketStatus.CLOSED) {
                ticket.setResolvedAt(LocalDateTime.now());
            }
            Ticket saved = ticketRepository.save(ticket);

            if (oldStatus != saved.getStatus()) {
                notifyStatusChange(saved, oldStatus, SystemUtils.getCurrentUsername());
            }
            return enrichCounts(ticketMapper.toResponse(saved));
        } catch (IllegalArgumentException e) {
            throw new AppException(TaskErrorCode.TICKET_STATUS_INVALID);
        }
    }

    @Override
    @Transactional
    public TicketResponse assignTicket(String id, String assigneeId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new AppException(TaskErrorCode.TICKET_NOT_FOUND));

        String oldAssigneeId = ticket.getAssigneeId();
        ticket.setAssigneeId(assigneeId);
        if (ticket.getStatus() == Ticket.TicketStatus.OPEN) {
            ticket.setStatus(Ticket.TicketStatus.IN_PROGRESS);
        }
        Ticket saved = ticketRepository.save(ticket);

        if (!Objects.equals(oldAssigneeId, saved.getAssigneeId())) {
            notifyAssignment(saved, oldAssigneeId, SystemUtils.getCurrentUsername());
        }
        return enrichCounts(ticketMapper.toResponse(saved));
    }

    // ============================================================
    // Comment / attachment aggregates (board card counts)
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


    /** Gửi thông báo khi assignee đổi (giao mới hoặc đổi người). */
    private void notifyAssignment(Ticket ticket, String prevAssigneeId, String actor) {
        String actionUrl = ACTION_URL_PREFIX + ticket.getId();
        String ticketLabel = "#" + safe(ticket.getCode()) + " – " + safe(ticket.getTitle());
        String priority = mapPriorityLevel(ticket.getPriority());

        // 1. Assignee mới
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
                    /* urgent = */ isUrgent(ticket)
            );
        }

        // 2. Assignee cũ (nếu bị đổi giao cho người khác)
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

        // 3. Reporter (người giao) — trừ khi chính họ đang thao tác
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

        log.debug("Ticket {} assignment notified. priority={}", ticket.getCode(), priority);
    }

    /** Gửi thông báo khi status ticket thay đổi. */
    private void notifyStatusChange(Ticket ticket, Ticket.TicketStatus oldStatus, String actor) {
        String actionUrl = ACTION_URL_PREFIX + ticket.getId();
        String ticketLabel = "#" + safe(ticket.getCode()) + " – " + safe(ticket.getTitle());
        String statusText = statusLabel(ticket.getStatus());
        boolean urgent = ticket.getStatus() == Ticket.TicketStatus.RESOLVED
                || ticket.getStatus() == Ticket.TicketStatus.CLOSED
                || isUrgent(ticket);

        List<String> recipients = new ArrayList<>();
        // Reporter — luôn nhận (trừ chính họ)
        if (ticket.getReporterId() != null && !Objects.equals(ticket.getReporterId(), actor)) {
            recipients.add(ticket.getReporterId());
        }
        // Assignee — nhận nếu ngoài họ đổi trạng thái
        String assigneeUsername = resolveUsername(ticket.getAssigneeId());
        if (assigneeUsername != null && !Objects.equals(assigneeUsername, actor)) {
            recipients.add(assigneeUsername);
        }

        notificationService.notifyMany(
                recipients,
                "Ticket đổi trạng thái: " + statusText,
                "Ticket " + ticketLabel + " chuyển từ "
                        + statusLabel(oldStatus) + " → " + statusText + ".",
                "TICKET_STATUS_CHANGED",
                TICKET_ENTITY,
                ticket.getId(),
                actionUrl,
                actor,
                urgent
        );
    }


    private String resolveUsername(String personId) {
        if (personId == null || personId.isBlank()) return null;
        // Nếu personId thực ra đã là username (backward compat), thử findByUserName trước
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
            case RESOLVED -> "Đã giải quyết";
            case CLOSED -> "Đã đóng";
        };
    }

    private static String mapPriorityLevel(Ticket.TicketPriority p) {
        if (p == null) return "NORMAL";
        return switch (p) {
            case LOW -> "LOW";
            case MEDIUM -> "NORMAL";
            case HIGH -> "HIGH";
            case URGENT -> "URGENT";
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
