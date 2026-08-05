package com.frezo.qtht.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Pool riêng cho tác vụ nền cấu hình động — tách khỏi scheduler mặc định của Spring
 * để job chạy lâu không chặn hạ tầng khác.
 */
@Configuration
public class SystemJobSchedulerConfig {

    @Bean(name = "systemJobTaskScheduler", destroyMethod = "shutdown")
    public ThreadPoolTaskScheduler systemJobTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("system-job-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }
}
