package com.frezo.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bootstrap Frezo Backend.
 * <p>
 * <b>v1.1 fixes (Batch B):</b>
 * <ul>
 *   <li>Xoá {@code DriverManager.getConnection(...)} tự tạo database — không phải trách nhiệm của bootstrap class.
 *       Database phải tồn tại trước khi app khởi động (init qua Docker Compose / infra script / Flyway sẽ handle schema).</li>
 *   <li>Thêm {@code @ConfigurationPropertiesScan("com.frezo")} — auto-detect mọi {@code @ConfigurationProperties}
 *       thay vì phải khai báo thủ công (@EnableConfigurationProperties(...) từng lớp).</li>
 *   <li>Thêm {@code @EnableAsync} — cho {@code @Async} email / notification / export.</li>
 * </ul>
 * <p>
 * <b>Setup Postgres lần đầu (thay thế cho code cũ):</b>
 * <pre>
 * # macOS/Linux:
 * createdb -U postgres frezo
 *
 * # Windows PowerShell (với psql):
 * psql -U postgres -c "CREATE DATABASE frezo"
 *
 * # Docker Compose: đã có sẵn trong docker-compose.yml (service postgres tự tạo DB frezo)
 * </pre>
 */
@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
@ConfigurationPropertiesScan(basePackages = "com.frezo")
@ComponentScan(basePackages = { "com.frezo" })
@EntityScan(basePackages = {
        "com.frezo.auth.entity",
        "com.frezo.qtht.entity",
        "com.frezo.dmdc.entity",
        "com.frezo.qtbv.entity",
        "com.frezo.product.entity",
        "com.frezo.email.entity",
        "com.frezo.task.entity",
        "com.frezo.qlns.entity",
        "com.frezo.customer.entity",
        "com.frezo.warehouse.entity",
        "com.frezo.fbautomation.entity",
        "com.frezo.accounting.entity",
        "com.frezo.crm.entity",
        "com.frezo.approval.entity",
        "com.frezo.event.entity",
        "com.frezo.common.domain",
        "com.frezo.common.entity",
        "com.frezo.common.audit",
        // Generic Workflow Engine (Definition / Step / Instance / Task) —
        // dùng chung cho mọi module có approval flow.
        "com.frezo.common.workflow.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.frezo.auth.repository",
        "com.frezo.qtht.repository",
        "com.frezo.dmdc.repository",
        "com.frezo.qtbv.repository",
        "com.frezo.product.repository",
        "com.frezo.email.repository",
        "com.frezo.task.repository",
        "com.frezo.qlns.repository",
        "com.frezo.customer.repository",
        "com.frezo.warehouse.repository",
        "com.frezo.fbautomation.repository",
        "com.frezo.accounting.repository",
        "com.frezo.crm.repository",
        "com.frezo.approval.repository",
        "com.frezo.event.repository",
        "com.frezo.common.repository",
        "com.frezo.common.audit",
        "com.frezo.common.workflow.repository"
})
public class FrezoServerApplication {

    public static void main(String[] args) {
        System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(FrezoServerApplication.class, args);
    }
}
