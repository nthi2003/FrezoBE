package com.frezo.crm.job;

import com.frezo.common.scheduling.SchedulableJob;
import com.frezo.crm.service.EmailSequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSequenceJob implements SchedulableJob {

    private final EmailSequenceService emailSequenceService;

    @Override
    public String getCode() {
        return "EMAIL_SEQUENCE";
    }

    @Override
    public String getDisplayName() {
        return "Gửi email sequence đến hạn";
    }

    @Override
    public String getDescription() {
        return "Quét các bước email sequence tới hạn và gửi cho khách hàng";
    }

    @Override
    public String getModuleCode() {
        return "CRM";
    }

    @Override
    public String getDefaultCron() {
        return "0 0 * * * *";
    }

    @Override
    public void execute() {
        processDue();
    }

    public void processDue() {
        log.debug("[EmailSequenceJob] process due steps");
        emailSequenceService.processDueSteps();
    }
}
