package com.frezo.accounting.service.impl;

import com.frezo.accounting.common.AccountingStandard;
import com.frezo.accounting.dto.request.AccountingSettingRequest;
import com.frezo.accounting.dto.response.AccountingSettingResponse;
import com.frezo.accounting.entity.AccountingSetting;
import com.frezo.accounting.repository.AccountingSettingRepository;
import com.frezo.accounting.service.AccountService;
import com.frezo.accounting.service.AccountingSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountingSettingServiceImpl implements AccountingSettingService {

    private final AccountingSettingRepository repo;
    private final AccountService accountService;

    @Override
    @Transactional
    public AccountingSetting getOrCreateDefault() {
        return repo.findFirstByIsDeletedFalse().orElseGet(() -> {
            AccountingSetting s = AccountingSetting.builder()
                    .standard(AccountingStandard.TT133)
                    .baseCurrency("VND")
                    .payrollPostingStrategy("AGGREGATE_PERIOD")
                    .accSalaryExpense("6421")
                    .accSalaryPayable("334")
                    .accBhxhPayable("3383")
                    .accBhytPayable("3384")
                    .accBhtnPayable("3385")
                    .accPitPayable("3335")
                    .accUnionFee("3382")
                    .build();
            s.setIsDeleted(false);
            return repo.save(s);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public AccountingSettingResponse view() {
        return toResponse(getOrCreateDefault());
    }

    @Override
    @Transactional
    public AccountingSettingResponse update(AccountingSettingRequest req) {
        AccountingSetting s = getOrCreateDefault();
        AccountingStandard oldStd = s.getStandard();
        s.setStandard(req.getStandard());
        if (req.getBaseCurrency() != null) s.setBaseCurrency(req.getBaseCurrency());
        if (req.getPayrollPostingStrategy() != null)
            s.setPayrollPostingStrategy(req.getPayrollPostingStrategy());
        if (req.getAccSalaryExpense() != null) s.setAccSalaryExpense(req.getAccSalaryExpense());
        if (req.getAccSalaryPayable() != null) s.setAccSalaryPayable(req.getAccSalaryPayable());
        if (req.getAccBhxhPayable() != null) s.setAccBhxhPayable(req.getAccBhxhPayable());
        if (req.getAccBhytPayable() != null) s.setAccBhytPayable(req.getAccBhytPayable());
        if (req.getAccBhtnPayable() != null) s.setAccBhtnPayable(req.getAccBhtnPayable());
        if (req.getAccPitPayable() != null) s.setAccPitPayable(req.getAccPitPayable());
        if (req.getAccUnionFee() != null) s.setAccUnionFee(req.getAccUnionFee());
        AccountingSetting saved = repo.save(s);

        // Áp default TK theo standard khi chuyển chuẩn (nếu user chưa override)
        if (oldStd != req.getStandard()) {
            applyStandardDefaults(saved, req.getStandard());
            saved = repo.save(saved);
        }

        // Seed COA nếu request yêu cầu
        if (Boolean.TRUE.equals(req.getSeedCoa())) {
            accountService.seedChartOfAccounts(req.getStandard());
        }
        return toResponse(saved);
    }

    private void applyStandardDefaults(AccountingSetting s, AccountingStandard std) {
        // TT133: BHTN = 3385. TT99 (như TT200): BHTN = 3386. Salary expense: TT133 = 6421, TT99 = 642.
        if (std == AccountingStandard.TT99) {
            if (s.getAccSalaryExpense() == null || "6421".equals(s.getAccSalaryExpense())) {
                s.setAccSalaryExpense("642");
            }
            if (s.getAccBhtnPayable() == null || "3385".equals(s.getAccBhtnPayable())) {
                s.setAccBhtnPayable("3386");
            }
        } else {
            if (s.getAccSalaryExpense() == null || "642".equals(s.getAccSalaryExpense())) {
                s.setAccSalaryExpense("6421");
            }
            if (s.getAccBhtnPayable() == null || "3386".equals(s.getAccBhtnPayable())) {
                s.setAccBhtnPayable("3385");
            }
        }
    }

    private AccountingSettingResponse toResponse(AccountingSetting s) {
        return AccountingSettingResponse.builder()
                .id(s.getId())
                .standard(s.getStandard())
                .baseCurrency(s.getBaseCurrency())
                .payrollPostingStrategy(s.getPayrollPostingStrategy())
                .accSalaryExpense(s.getAccSalaryExpense())
                .accSalaryPayable(s.getAccSalaryPayable())
                .accBhxhPayable(s.getAccBhxhPayable())
                .accBhytPayable(s.getAccBhytPayable())
                .accBhtnPayable(s.getAccBhtnPayable())
                .accPitPayable(s.getAccPitPayable())
                .accUnionFee(s.getAccUnionFee())
                .build();
    }
}
