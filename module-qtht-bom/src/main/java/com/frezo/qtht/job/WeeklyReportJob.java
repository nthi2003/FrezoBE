package com.frezo.qtht.job;

import com.frezo.common.scheduling.SchedulableJob;
import com.frezo.qtht.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Gửi báo cáo tổng hợp đầu tuần cho admin. */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyReportJob implements SchedulableJob {

    private final ReportService reportService;

    @Override
    public String getCode() {
        return "WEEKLY_REPORT";
    }

    @Override
    public String getDisplayName() {
        return "Báo cáo tuần tự động";
    }

    @Override
    public String getDescription() {
        return "Tổng hợp số liệu tuần và gửi email cho quản trị viên";
    }

    @Override
    public String getModuleCode() {
        return "QTHT";
    }

    @Override
    public String getDefaultCron() {
        return "0 0 8 * * MON";
    }

    @Override
    public void execute() {
        reportService.sendWeeklyReport();
    }
}
