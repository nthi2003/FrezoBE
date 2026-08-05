package com.frezo.warehouse.job;

import com.frezo.common.scheduling.SchedulableJob;
import com.frezo.warehouse.entity.StockAlert;
import com.frezo.warehouse.repository.StockAlertRepository;
import com.frezo.warehouse.service.ExpiryAlertService;
import com.frezo.warehouse.service.ReorderService;
import com.frezo.warehouse.service.StockAlertNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockAlertJob implements SchedulableJob {

    private final ReorderService reorderService;
    private final StockAlertRepository alertRepository;
    private final ExpiryAlertService expiryAlertService;
    private final StockAlertNotifier stockAlertNotifier;

    @Override
    public String getCode() {
        return "STOCK_ALERT_SCAN";
    }

    @Override
    public String getDisplayName() {
        return "Quét cảnh báo tồn kho";
    }

    @Override
    public String getDescription() {
        return "Quét quy tắc tồn kho tối thiểu và lô hàng cận hạn để sinh cảnh báo và gửi thông báo";
    }

    @Override
    public String getModuleCode() {
        return "WAREHOUSE";
    }

    @Override
    public String getDefaultCron() {
        return "0 0 6 * * *";
    }

    @Override
    public void execute() {
        runMorningScan();
    }

    /**
     * Scan ReorderRule + lô cận hạn → StockAlert OPEN + notify từng alert.
     * Gọi thủ công qua POST /warehouse/stock-alerts/scan, hoặc theo lịch trong bảng system_job.
     */
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
