package com.frezo.accounting.service.impl;

import com.frezo.accounting.entity.PayslipConfirmation;
import com.frezo.accounting.repository.PayslipConfirmationRepository;
import com.frezo.accounting.service.PayslipConfirmationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PayslipConfirmationServiceImpl implements PayslipConfirmationService {

    private final PayslipConfirmationRepository repo;

    @Override
    @Transactional(readOnly = true)
    public Optional<PayslipConfirmation> find(String payrollId) {
        return repo.findByPayrollId(payrollId);
    }

    @Override
    @Transactional
    public PayslipConfirmation confirm(String payrollId, String personId, String note,
                                       String ip, String device) {
        PayslipConfirmation pc = repo.findByPayrollId(payrollId).orElseGet(() -> {
            PayslipConfirmation newPc = PayslipConfirmation.builder()
                    .payrollId(payrollId)
                    .personId(personId)
                    .build();
            newPc.setIsDeleted(false);
            return newPc;
        });
        pc.setConfirmedAt(LocalDateTime.now());
        pc.setConfirmedFromIp(ip);
        pc.setConfirmedFromDevice(device);
        if (note != null) pc.setNote(note);
        return repo.save(pc);
    }
}
