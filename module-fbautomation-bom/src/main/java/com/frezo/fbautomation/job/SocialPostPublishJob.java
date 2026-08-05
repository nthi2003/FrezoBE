package com.frezo.fbautomation.job;

import com.frezo.common.scheduling.SchedulableJob;
import com.frezo.fbautomation.service.SocialPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Đăng bài social tới giờ hẹn — wrapper lịch cho {@link SocialPostService#runScheduler()}. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocialPostPublishJob implements SchedulableJob {

    private final SocialPostService socialPostService;

    @Override
    public String getCode() {
        return "SOCIAL_POST_PUBLISH";
    }

    @Override
    public String getDisplayName() {
        return "Đăng bài social đã hẹn giờ";
    }

    @Override
    public String getDescription() {
        return "Quét bài viết trạng thái SCHEDULED tới giờ đăng và publish lên kênh tương ứng";
    }

    @Override
    public String getModuleCode() {
        return "FB_AUTOMATION";
    }

    @Override
    public String getDefaultCron() {
        return "0 * * * * *";
    }

    @Override
    public void execute() {
        socialPostService.runScheduler();
    }
}
