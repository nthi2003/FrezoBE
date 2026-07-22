package com.frezo.crm.job;

import com.frezo.crm.service.EmailSequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSequenceJob {

    private final EmailSequenceService emailSequenceService;

    /** Mỗi giờ — stub gửi bước sequence đến hạn. */
    @Scheduled(cron = "0 0 * * * *")
    public void processDue() {
        log.debug("[EmailSequenceJob] process due steps");
        emailSequenceService.processDueSteps();
    }
}
