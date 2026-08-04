package com.frezo.crm.service;

import com.frezo.crm.entity.CommissionEntry;
import com.frezo.crm.entity.CommissionRule;
import com.frezo.crm.entity.Invoice;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CommissionService {

    List<CommissionRule> listRules();

    CommissionRule upsertRule(String salespersonUsername, BigDecimal ratePercent, Boolean active, String note);

    void deleteRule(String id);

    /** Resolve %: rule theo sale → rule mặc định (*) → 5%. */
    BigDecimal resolveRatePercent(String salespersonUsername);

    /** Gắn salesperson + rate + amount lên invoice (khi tạo/sửa). */
    void applyCommissionFields(Invoice invoice, String salespersonOverride, BigDecimal rateOverride);

    /** Accrue entry khi HĐ đã thu tiền (PAID / PARTIALLY_PAID). */
    CommissionEntry accrueFromInvoice(Invoice invoice);

    List<CommissionEntry> listEntries(String salespersonUsername);

    CommissionEntry approve(String id);

    CommissionEntry markPaid(String id);

    CommissionEntry voidEntry(String id);

    Map<String, Object> dashboard();
}
