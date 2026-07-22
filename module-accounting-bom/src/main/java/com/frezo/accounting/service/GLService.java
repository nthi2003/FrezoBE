package com.frezo.accounting.service;

import com.frezo.accounting.dto.response.GLResponse;
import com.frezo.accounting.dto.response.TrialBalanceRowResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Sổ cái + Bảng cân đối phát sinh — chỉ đọc, tính từ JournalEntryLine.
 */
public interface GLService {

    /** Sổ cái của 1 tài khoản trong khoảng thời gian. */
    GLResponse getLedger(String accountCode, LocalDate from, LocalDate to);

    /** Bảng cân đối phát sinh trong khoảng thời gian. */
    List<TrialBalanceRowResponse> trialBalance(LocalDate from, LocalDate to);
}
