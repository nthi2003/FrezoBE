package com.frezo.crm.config;

import com.frezo.crm.entity.CommissionRule;
import com.frezo.crm.repository.CommissionRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Seed mức hoa hồng mặc định 5% nếu chưa có. */
@Slf4j
@Component
@Order(55)
@RequiredArgsConstructor
public class CommissionRuleSeedRunner implements ApplicationRunner {

    private final CommissionRuleRepository ruleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (ruleRepository.findBySalespersonUsernameAndIsDeletedFalse(CommissionRule.DEFAULT_USERNAME).isPresent()) {
            return;
        }
        ruleRepository.save(CommissionRule.builder()
                .salespersonUsername(CommissionRule.DEFAULT_USERNAME)
                .ratePercent(new BigDecimal("5.00"))
                .active(true)
                .note("Mức hoa hồng mặc định toàn hệ thống")
                .build());
        log.info("Seeded default commission rule 5%");
    }
}
