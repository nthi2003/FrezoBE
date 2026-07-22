package com.frezo.task.service.impl;

import com.frezo.auth.repository.UserRepository;
import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.service.NotificationService;
import com.frezo.task.dto.request.TicketRequest;
import com.frezo.task.dto.response.TicketResponse;
import com.frezo.task.entity.Ticket;
import com.frezo.task.mapper.TicketMapper;
import com.frezo.task.repository.TicketRepository;
import com.frezo.task.service.TicketService;
import com.frezo.util.web.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
 * Notification bao gồm deep-link {@code /tasks?ticketId=...} để FE navigate ngay khi click.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final String TICKET_ENTITY = "TICKET";
    private static final String ACTION_URL_PREFIX = "/tasks?ticketId=";

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // ============================================================
    // CRUD
    // ============================================================

    @Override
    @Transactional
    public Response<TicketResponse> create(TicketRequest request) {
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

        return Response.ok(ticketMapper.toResponse(saved));
    }

    @Override
    @Transactional
    public Response<TicketResponse> update(String id, TicketRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new QTHTException("Ticket not found"));

        // Snapshot trước khi update để so sánh
        Ticket.TicketStatus oldStatus = ticket.getStatus();
        String oldAssigneeId = ticket.getAssigneeId();

        ticketMapper.updateEntityFromRequest(request, ticket);

        if (ticket.getStatus() == Ticket.TicketStatus.RESOLVED || ticket.getStatus() == Ticket.TicketStatus.CLOSED) {
            if (ticket.getResolvedAt() == null) {
                ticket.setResolvedAt(LocalDateTime.now());
            }
        }

        Ticket saved = ticketRepository.save(ticket);

        String currentUser = SystemUtils.getCurrentUsername();

        // Assignee thay đổi ⇒ báo assignee mới + reporter
        if (!Objects.equals(oldAssigneeId, saved.getAssigneeId())) {
            notifyAssignment(saved, oldAssigneeId, currentUser);
        }
        // Status thay đổi ⇒ báo reporter + assignee
        if (oldStatus != saved.getStatus()) {
            notifyStatusChange(saved, oldStatus, currentUser);
        }

        return Response.ok(ticketMapper.toResponse(saved));
    }

    @Override
    @Transactional
    public void delete(String id) {
        if (!ticketRepository.existsById(id)) {
            throw new QTHTException("Ticket not found");
        }
        ticketRepository.deleteById(id);
    }

    @Override
    public Response<TicketResponse> findById(String id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new QTHTException("Ticket not found"));
        return Response.ok(ticketMapper.toResponse(ticket));
    }

    @Override
    public Response<List<TicketResponse>> findAll() {
        List<Ticket> tickets = ticketRepository.findAll();
        return Response.ok(ticketMapper.toResponseList(tickets));
    }

    // ============================================================
    // Status + Assignment (dedicated endpoints)
    // ============================================================

    @Override
    @Transactional
    public Response<TicketResponse> updateStatus(String id, String status) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new QTHTException("Ticket not found"));

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
            return Response.ok(ticketMapper.toResponse(saved));
        } catch (IllegalArgumentException e) {
            throw new QTHTException("Invalid ticket status");
        }
    }

    @Override
    @Transactional
    public Response<TicketResponse> assignTicket(String id, String assigneeId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new QTHTException("Ticket not found"));

        String oldAssigneeId = ticket.getAssigneeId();
        ticket.setAssigneeId(assigneeId);
        if (ticket.getStatus() == Ticket.TicketStatus.OPEN) {
            ticket.setStatus(Ticket.TicketStatus.IN_PROGRESS);
        }
        Ticket saved = ticketRepository.save(ticket);

        if (!Objects.equals(oldAssigneeId, saved.getAssigneeId())) {
            notifyAssignment(saved, oldAssigneeId, SystemUtils.getCurrentUsername());
        }
        return Response.ok(ticketMapper.toResponse(saved));
    }

    // ============================================================
    // Notification helpers
    // ============================================================

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

    /** Resolve personId → username qua UserRepository. */
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
