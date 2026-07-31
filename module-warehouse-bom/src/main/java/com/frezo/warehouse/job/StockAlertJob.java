package com.frezo.warehouse.job;

import com.frezo.warehouse.entity.StockAlert;
import com.frezo.warehouse.repository.StockAlertRepository;
import com.frezo.warehouse.service.ExpiryAlertService;
import com.frezo.warehouse.service.ReorderService;
import com.frezo.warehouse.service.StockAlertNotifier;
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
    private final ExpiryAlertService expiryAlertService;
    private final StockAlertNotifier stockAlertNotifier;

    /**
     * 06:00 mỗi ngày — scan ReorderRule + lô cận hạn → StockAlert OPEN + notify từng alert.
     * Có thể gọi {@link #runMorningScan()} thủ công qua POST /warehouse/stock-alerts/scan.
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void runMorningScan() {
        log.info("[StockAlertJob] start morning scan");
        reorderService.scanAndRaiseAlerts();
        expiryAlertService.scanAndRaiseExpiryAlerts();
        notifyTodaysAlerts();
    }

    /** Safety-net: notify OPEN alerts raised hôm nay (idempotent nếu đã notify on-create). */
    private void notifyTodaysAlerts() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
        List<StockAlert> today = alertRepository.findByStatusAndIsDeletedFalseOrderByTriggeredAtDesc("OPEN")
                .stream()
                .filter(a -> a.getTriggeredAt() != null
                        && !a.getTriggeredAt().isBefore(start)
                        && !a.getTriggeredAt().isAfter(end))
                .toList();
        if (today.isEmpty()) {
            log.info("[StockAlertJob] no new OPEN alerts today — skip notify");
            return;
        }
        stockAlertNotifier.notifyAlerts(today);
        log.info("[StockAlertJob] notified recipients about {} alerts", today.size());
    }
}
