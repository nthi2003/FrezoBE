package com.frezo.profit.service.support;

import com.frezo.profit.common.Salary3P;
import com.frezo.profit.entity.ProfitSetting;
import com.frezo.qlns.common.StatusContarct;
import com.frezo.qlns.entity.Contract;
import com.frezo.qlns.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProfitContractSalaryResolver {

    private final ContractRepository contractRepository;

    public Optional<Contract> findActiveContract(String personId, String contractId) {
        if (contractId != null && !contractId.isBlank()) {
            return contractRepository.findById(contractId)
                    .filter(c -> c.getIsDeleted() == null || !c.getIsDeleted())
                    .filter(this::isEligible);
        }
        if (personId == null || personId.isBlank()) {
            return Optional.empty();
        }
        return contractRepository.findAll().stream()
                .filter(c -> personId.equals(c.getPersonId()))
                .filter(c -> c.getIsDeleted() == null || !c.getIsDeleted())
                .filter(this::isEligible)
                .max(Comparator.comparing(Contract::getUpdatedDate, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    public Salary3P resolve3P(String personId, String contractId) {
        return findActiveContract(personId, contractId)
                .map(Salary3P::fromContract)
                .orElse(new Salary3P(0, 0, 0));
    }

    public double resolveRevenueTarget(Salary3P salary, ProfitSetting global, Double commissionOverride) {
        double cogs = pct(global.getDefaultCogsPct(), 55d);
        double risk = pct(global.getRiskCostPct(), 2d);
        double comm = commissionOverride != null
                ? commissionOverride
                : pct(global.getSalesCommissionPct(), 3d);
        return Salary3P.revenueTargetForFullKpi(salary, cogs, risk, comm);
    }

    private boolean isEligible(Contract c) {
        if (Boolean.TRUE.equals(c.getActivated())) {
            return true;
        }
        return c.getStatus() == StatusContarct.ACTIVE;
    }

    private static double pct(Double v, double fallback) {
        return v != null ? v : fallback;
    }
}
