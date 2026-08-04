package com.frezo.task.security;

import com.frezo.auth.repository.UserRepository;
import com.frezo.common.helper.SystemUtils;
import com.frezo.task.entity.Task;
import com.frezo.task.entity.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * Visibility + role helpers for Task/Ticket.
 * <ul>
 *   <li>Admin — thấy tất cả</li>
 *   <li>Người giao (reporter / createdBy) — thấy ticket/task họ đã giao</li>
 *   <li>Người nhận (assignee) — thấy ticket/task được giao cho mình</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskAccessHelper {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public String currentUsername() {
        return SystemUtils.getCurrentUsername();
    }

    public Optional<String> currentPersonId() {
        String username = currentUsername();
        if (username == null || username.isBlank()) return Optional.empty();
        return userRepository.findByUserName(username)
                .map(u -> u.getPersonId())
                .filter(id -> id != null && !id.isBlank());
    }

    /**
     * Admin = bootstrap usernames hoặc {@code Person.isAdmin=true}.
     */
    public boolean isAdmin() {
        String username = currentUsername();
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
            log.warn("[task] isAdmin check failed for '{}': {}", username, ex.getMessage());
            return false;
        }
    }

    public boolean canViewTicket(Ticket ticket) {
        if (ticket == null) return false;
        if (isAdmin()) return true;
        return isReporter(ticket) || isAssignee(ticket.getAssigneeId());
    }

    public boolean canViewTask(Task task) {
        if (task == null) return false;
        if (isAdmin()) return true;
        return isCreator(task.getCreatedBy()) || isAssignee(task.getAssigneeId());
    }

    /** Người giao / admin được duyệt hoàn thành (RESOLVED → CLOSED / reject). */
    public boolean canReviewTicket(Ticket ticket) {
        if (ticket == null) return false;
        return isAdmin() || isReporter(ticket);
    }

    /** Người tạo task / admin duyệt DONE → CLOSED. */
    public boolean canReviewTask(Task task) {
        if (task == null) return false;
        return isAdmin() || isCreator(task.getCreatedBy());
    }

    /** Assignee (hoặc admin) được đánh dấu hoàn thành. */
    public boolean canCompleteTicket(Ticket ticket) {
        if (ticket == null) return false;
        return isAdmin() || isAssignee(ticket.getAssigneeId());
    }

    public boolean canCompleteTask(Task task) {
        if (task == null) return false;
        return isAdmin() || isAssignee(task.getAssigneeId());
    }

    public boolean isReporter(Ticket ticket) {
        String me = currentUsername();
        if (me == null || ticket == null) return false;
        String reporter = ticket.getReporterId();
        if (idEquals(reporter, me)) return true;
        // demo/legacy: reporter_id có thể là personId
        return currentPersonId().map(pid -> idEquals(reporter, pid)).orElse(false)
                || idEquals(ticket.getCreatedBy(), me);
    }

    public boolean isCreator(String createdBy) {
        return idEquals(createdBy, currentUsername());
    }

    public boolean isAssignee(String assigneeId) {
        if (assigneeId == null || assigneeId.isBlank()) return false;
        String me = currentUsername();
        if (idEquals(assigneeId, me)) return true;
        return currentPersonId().map(pid -> idEquals(assigneeId, pid)).orElse(false);
    }

    private static boolean idEquals(String a, String b) {
        return a != null && b != null && Objects.equals(a.trim(), b.trim());
    }
}
