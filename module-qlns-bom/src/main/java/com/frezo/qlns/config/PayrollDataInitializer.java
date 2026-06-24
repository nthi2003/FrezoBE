package com.frezo.qlns.config;

import com.frezo.qlns.entity.InsuranceConfig;
import com.frezo.qlns.entity.TaxConfig;
import com.frezo.qlns.repository.InsuranceConfigRepository;
import com.frezo.qlns.repository.TaxConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayrollDataInitializer {

    private final InsuranceConfigRepository insuranceConfigRepository;
    private final TaxConfigRepository taxConfigRepository;

    @PostConstruct
    @Transactional
    public void init() {
        initInsuranceConfig();
        initTaxConfig();
    }

    private void initInsuranceConfig() {
        if (insuranceConfigRepository.findByYearAndIsActiveTrue(2025).isPresent()) {
            log.info("Insurance config for 2025 already exists, skipping seed.");
            return;
        }
        log.info("Seeding insurance config for 2025...");
        InsuranceConfig config = InsuranceConfig.builder()
                .year(2025)
                .socialInsuranceRate(new BigDecimal("0.0800"))
                .healthInsuranceRate(new BigDecimal("0.0150"))
                .unemploymentInsuranceRate(new BigDecimal("0.0100"))
                .employerSocialRate(new BigDecimal("0.1750"))
                .employerHealthRate(new BigDecimal("0.0300"))
                .employerUnemploymentRate(new BigDecimal("0.0100"))
                .employerAccidentRate(new BigDecimal("0.0050"))
                .maxInsuranceSalary(new BigDecimal("46800000"))
                .baseMinimumWage(new BigDecimal("2340000"))
                .regionalMinimumWage(new BigDecimal("4960000"))
                .region("I")
                .appliesFrom(1)
                .appliesTo(12)
                .isActive(true)
                .build();
        insuranceConfigRepository.save(config);
        log.info("Seeded insurance config for 2025.");
    }

    private void initTaxConfig() {
        if (!taxConfigRepository.findByYearAndIsActiveTrueOrderByBracketOrderAsc(2025).isEmpty()) {
            log.info("Tax config for 2025 already exists, skipping seed.");
            return;
        }
        log.info("Seeding tax brackets for 2025...");

        int year = 2025;
        BigDecimal personalDeduction = new BigDecimal("11000000");
        BigDecimal dependentDeduction = new BigDecimal("4400000");

        saveTaxBracket(year, 1, BigDecimal.ZERO, new BigDecimal("5000000"), new BigDecimal("0.05"),
                new BigDecimal("0"), personalDeduction, dependentDeduction);
        saveTaxBracket(year, 2, new BigDecimal("5000000"), new BigDecimal("10000000"), new BigDecimal("0.10"),
                new BigDecimal("250000"), personalDeduction, dependentDeduction);
        saveTaxBracket(year, 3, new BigDecimal("10000000"), new BigDecimal("18000000"), new BigDecimal("0.15"),
                new BigDecimal("750000"), personalDeduction, dependentDeduction);
        saveTaxBracket(year, 4, new BigDecimal("18000000"), new BigDecimal("32000000"), new BigDecimal("0.20"),
                new BigDecimal("1650000"), personalDeduction, dependentDeduction);
        saveTaxBracket(year, 5, new BigDecimal("32000000"), new BigDecimal("52000000"), new BigDecimal("0.25"),
                new BigDecimal("3250000"), personalDeduction, dependentDeduction);
        saveTaxBracket(year, 6, new BigDecimal("52000000"), new BigDecimal("80000000"), new BigDecimal("0.30"),
                new BigDecimal("5850000"), personalDeduction, dependentDeduction);
        saveTaxBracket(year, 7, new BigDecimal("80000000"), null, new BigDecimal("0.35"),
                new BigDecimal("12150000"), personalDeduction, dependentDeduction);

        log.info("Seeded 7 tax brackets for 2025.");
    }

    private void saveTaxBracket(Integer year, Integer order, BigDecimal from, BigDecimal to,
                                 BigDecimal rate, BigDecimal deduct, BigDecimal personalDed, BigDecimal dependentDed) {
        TaxConfig config = TaxConfig.builder()
                .year(year)
                .bracketOrder(order)
                .fromAmount(from)
                .toAmount(to)
                .rate(rate)
                .deductAmount(deduct)
                .personalDeduction(personalDed)
                .dependentDeduction(dependentDed)
                .isActive(true)
                .build();
        taxConfigRepository.save(config);
    }
}
