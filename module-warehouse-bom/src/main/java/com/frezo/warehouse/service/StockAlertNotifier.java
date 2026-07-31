package com.frezo.warehouse.service;

import com.frezo.approval.service.ApproverResolver;
import com.frezo.common.repository.NotificationRepository;
import com.frezo.common.service.NotificationService;
import com.frezo.product.entity.Product;
import com.frezo.product.repository.ProductRepository;
import com.frezo.warehouse.entity.StockAlert;
import com.frezo.warehouse.entity.Warehouse;
import com.frezo.warehouse.repository.StockBatchRepository;
import com.frezo.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Emit per-alert Notification (chuông) for LOW_STOCK / EXPIRY_SOON.
 * Recipients = ADMIN + users có menu WH_ALERTS.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockAlertNotifier {

    private static final String ACTION_URL = "/warehouse/stock-alerts";
    private static final String ENTITY_TYPE = "STOCK_ALERT";
    private static final String MENU_WH_ALERTS = "WH_ALERTS";
    private static final int MAX_PER_USER_PER_DAY = 30;

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final ApproverResolver approverResolver;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockBatchRepository stockBatchRepository;
    private final JdbcTemplate jdbcTemplate;

    /** Notify ngay khi tạo 1 alert (real-time / on-create). */
    public void notifyAlert(StockAlert alert) {
        if (alert == null || alert.getId() == null) return;
        notifyAlerts(List.of(alert));
    }

    /**
     * Gửi notification theo từng alert; cap 30/user/ngày, phần dư gộp 1 dòng tổng.
     * Idempotent theo (username, entityId) trong ngày.
     */
    public void notifyAlerts(List<StockAlert> alerts) {
        if (alerts == null || alerts.isEmpty()) return;
        List<String> recipients = resolveRecipients();
        if (recipients.isEmpty()) return;

        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        for (String username : recipients) {
            List<StockAlert> pending = new ArrayList<>();
            for (StockAlert a : alerts) {
                if (a == null || a.getId() == null) continue;
                if (alreadyNotifiedToday(username, a.getId(), dayStart)) continue;
                pending.add(a);
            }
            if (pending.isEmpty()) continue;

            long sentToday = notificationRepository
                    .countByUsernameAndEntityTypeAndCreatedAtGreaterThanEqual(username, ENTITY_TYPE, dayStart);
            int remaining = (int) Math.max(0, MAX_PER_USER_PER_DAY - sentToday);

            List<StockAlert> head = remaining > 0
                    ? pending.subList(0, Math.min(remaining, pending.size()))
                    : List.of();
            List<StockAlert> overflow = remaining < pending.size()
                    ? pending.subList(Math.min(remaining, pending.size()), pending.size())
                    : List.of();

            for (StockAlert a : head) {
                emitOne(username, a);
            }
            if (!overflow.isEmpty()) {
                String summaryId = "SUMMARY|" + LocalDate.now();
                if (!alreadyNotifiedToday(username, summaryId, dayStart)) {
                    notificationService.notify(
                            username,
                            "Cảnh báo tồn kho",
                            "+" + overflow.size() + " cảnh báo khác — xem Cảnh báo tồn",
                            "STOCK_ALERT",
                            ENTITY_TYPE,
                            summaryId,
                            ACTION_URL,
                            "system",
                            overflow.stream().anyMatch(this::isUrgent));
                }
            }
        }
    }

    public List<String> resolveRecipients() {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (ApproverResolver.ApproverHint h : approverResolver.resolveAll("ADMIN")) {
            if (h.username() != null && !h.username().isBlank()) {
                out.add(h.username().trim());
            }
        }
        out.addAll(findUsernamesByMenuCode(MENU_WH_ALERTS));
        if (out.isEmpty()) {
            out.add("admin");
        }
        return new ArrayList<>(out);
    }

    private void emitOne(String username, StockAlert alert) {
        String alertType = normalizeType(alert.getAlertType());
        String notifType = "EXPIRY_SOON".equals(alertType) ? "STOCK_EXPIRY" : "STOCK_LOW";
        String title;
        String message;
        if ("EXPIRY_SOON".equals(alertType)) {
            String batchLabel = resolveBatchLabel(alert);
            title = "Sắp hết hạn: lô " + batchLabel;
            message = buildExpiryMessage(alert);
        } else {
            String productLabel = resolveProductLabel(alert.getProductId());
            title = "Sắp hết hàng: " + productLabel;
            message = buildLowStockMessage(alert);
        }
        notificationService.notify(
                username,
                title,
                message,
                notifType,
                ENTITY_TYPE,
                alert.getId(),
                ACTION_URL,
                "system",
                isUrgent(alert));
    }

    private boolean alreadyNotifiedToday(String username, String entityId, LocalDateTime dayStart) {
        return notificationRepository.existsByUsernameAndEntityTypeAndEntityIdAndCreatedAtGreaterThanEqual(
                username, ENTITY_TYPE, entityId, dayStart);
    }

    private boolean isUrgent(StockAlert a) {
        return "CRITICAL".equalsIgnoreCase(a.getSeverity());
    }

    private String normalizeType(String alertType) {
        if (alertType == null || alertType.isBlank()) return "LOW_STOCK";
        return alertType.trim().toUpperCase(Locale.ROOT);
    }

    private String resolveProductLabel(String productId) {
        if (productId == null) return "?";
        Product p = productRepository.findById(productId).orElse(null);
        if (p == null) return shortId(productId);
        if (p.getCode() != null && !p.getCode().isBlank()) return p.getCode();
        if (p.getName() != null && !p.getName().isBlank()) return p.getName();
        return shortId(productId);
    }

    private String resolveBatchLabel(StockAlert alert) {
        if (alert.getBatchId() != null) {
            return stockBatchRepository.findById(alert.getBatchId())
                    .map(b -> b.getBatchCode() != null && !b.getBatchCode().isBlank()
                            ? b.getBatchCode()
                            : shortId(alert.getBatchId()))
                    .orElse(shortId(alert.getBatchId()));
        }
        return shortId(alert.getId());
    }

    private String buildLowStockMessage(StockAlert alert) {
        Warehouse wh = alert.getWarehouseId() != null
                ? warehouseRepository.findById(alert.getWarehouseId()).orElse(null)
                : null;
        String whLabel = wh != null
                ? (wh.getName() != null ? wh.getName() : wh.getCode())
                : (alert.getWarehouseId() != null ? shortId(alert.getWarehouseId()) : "?");
        return "Tồn " + fmtQty(alert.getCurrentQty())
                + " < min " + fmtQty(alert.getMinQty())
                + " · kho " + whLabel
                + ". Mở Cảnh báo tồn để tạo PR / xử lý.";
    }

    private String buildExpiryMessage(StockAlert alert) {
        String productCode = resolveProductLabel(alert.getProductId());
        String hsd = alert.getExpiryDate() != null ? alert.getExpiryDate().toString() : "?";
        int days = alert.getDaysToExpiry() != null ? alert.getDaysToExpiry() : 0;
        return productCode
                + " · HSD " + hsd
                + " · còn " + days + " ngày (qty " + fmtQty(alert.getCurrentQty()) + ")"
                + ". Kiểm tra xuất ưu tiên / hủy lô.";
    }

    private static String fmtQty(Double q) {
        if (q == null) return "0";
        if (q == Math.rint(q)) return String.valueOf(q.longValue());
        return String.valueOf(q);
    }

    private static String shortId(String id) {
        if (id == null || id.isBlank()) return "?";
        return id.length() <= 8 ? id : id.substring(0, 8);
    }

    private List<String> findUsernamesByMenuCode(String menuCode) {
        try {
            return jdbcTemplate.queryForList(
                    """
                    SELECT DISTINCT u.user_name
                    FROM menu m
                    JOIN role_menu rm ON rm.menu_id = m.id
                        AND COALESCE(rm.is_deleted, false) = false
                    JOIN user_role ur ON ur.role_id = rm.role_id
                        AND COALESCE(ur.is_deleted, false) = false
                    JOIN users u ON u.id = ur.user_id
                        AND COALESCE(u.is_deleted, false) = false
                    WHERE m.code = ?
                      AND m.app_code = 'QTHT'
                      AND COALESCE(m.is_deleted, false) = false
                      AND (u.status IS NULL OR u.status <> 0)
                    """,
                    String.class,
                    menuCode);
        } catch (Exception e) {
            log.warn("[StockAlertNotifier] resolve menu {} recipients failed: {}", menuCode, e.getMessage());
            return List.of();
        }
    }
}
