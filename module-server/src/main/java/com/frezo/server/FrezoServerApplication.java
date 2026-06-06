package com.frezo.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;




@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
@EnableScheduling
@ComponentScan(basePackages = { "com.frezo" })
//@MapperScan is not used here to keep configuration centralized in MapStructConfig


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
        "com.frezo.common.domain",
        "com.frezo.common.entity",
        "com.frezo.common.audit"
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
        "com.frezo.common.repository",
        "com.frezo.common.audit"
})
public class FrezoServerApplication {

    public static void main(String[] args) {
        System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(FrezoServerApplication.class, args);
    }
}
