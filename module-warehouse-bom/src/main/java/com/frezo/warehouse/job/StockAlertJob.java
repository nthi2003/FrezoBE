package com.frezo.warehouse.job;

import com.frezo.approval.service.ApproverResolver;
import com.frezo.common.service.NotificationService;
import com.frezo.warehouse.entity.StockAlert;
import com.frezo.warehouse.repository.StockAlertRepository;
import com.frezo.warehouse.service.ReorderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockAlertJob {

    private final ReorderService reorderService;
    private final StockAlertRepository alertRepository;
    private final NotificationService notificationService;
    private final ApproverResolver approverResolver;

    /** 06:00 mỗi ngày — scan ReorderRule active → raise StockAlert OPEN + notify. */
    @Scheduled(cron = "0 0 6 * * *")
    public void runMorningScan() {
        log.info("[StockAlertJob] start morning scan");
        reorderService.scanAndRaiseAlerts();
        notifyTodaysAlerts();
    }

    private void notifyTodaysAlerts() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
        List<StockAlert> today = alertRepository.findByStatusAndIsDeletedFalseOrderByTriggeredAtDesc("OPEN")
                .stream()
                .filter(a -> a.getTriggeredAt() != null
                        && !a.getTriggeredAt().isBefore(start)
                        && !a.getTriggeredAt().isAfter(end))
                .toList();
        if (today.isEmpty()) return;

        List<String> recipients = approverResolver.resolveAll("ADMIN").stream()
                .map(ApproverResolver.ApproverHint::username)
                .toList();
        if (recipients.isEmpty()) {
            recipients = List.of("admin");
        }
        String title = "Cảnh báo tồn kho thấp";
        String msg = today.size() + " alert mới hôm nay — kiểm tra Reorder";
        notificationService.notifyMany(
                recipients,
                title,
                msg,
                "STOCK_ALERT",
                "STOCK_ALERT",
                today.get(0).getId(),
                "/warehouse/stock-alerts",
                "system",
                today.stream().anyMatch(a -> "CRITICAL".equals(a.getSeverity())));
        log.info("[StockAlertJob] notified {} admins about {} alerts", recipients.size(), today.size());
    }
}
