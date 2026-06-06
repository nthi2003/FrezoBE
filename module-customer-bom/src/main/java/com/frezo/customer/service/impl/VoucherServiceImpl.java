package com.frezo.customer.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.utils.SecureCodeGenerator;
import com.frezo.customer.entity.Voucher;
import com.frezo.customer.repository.VoucherRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherServiceImpl {

    private final VoucherRepository voucherRepository;

    // ── List ─────────────────────────────────────────────────────────────────────
    public Map<String, Object> getAll(String keyword, String status, String discountType,
                                       Integer pageNumber, Integer pageSize) {
        Specification<Voucher> spec = Specification.where(null);
        if (SystemUtils.isNotNullOrEmpty(keyword)) {
            spec = spec.and(GenericSpecification.<Voucher>likeField("name", keyword)
                    .or(GenericSpecification.likeField("code", keyword)));
        }
        if (SystemUtils.isNotNullOrEmpty(status)) {
            spec = spec.and(GenericSpecification.equalField("status", status));
        }
        if (SystemUtils.isNotNullOrEmpty(discountType)) {
            spec = spec.and(GenericSpecification.equalField("discountType", discountType));
        }
        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate");
        Page<Voucher> page = voucherRepository.findAll(spec,
                ServiceHelper.createPageable(pageNumber, pageSize, sort));
        return ServiceHelper.createResponse1(pageNumber, pageSize, page, page.getContent());
    }

    // ── Create ────────────────────────────────────────────────────────────────────
    @Transactional
    public Voucher create(Voucher request) {
        if (!SystemUtils.isNotNullOrEmpty(request.getCode())) {
            String code;
            do { code = SecureCodeGenerator.generateCode("VC"); }
            while (voucherRepository.existsByCode(code));
            request.setCode(code);
        } else if (voucherRepository.existsByCode(request.getCode())) {
            throw new QTHTException("exception.voucher.code.exists", request.getCode());
        }
        if (request.getStatus() == null) request.setStatus("ACTIVE");
        if (request.getUsedCount() == null) request.setUsedCount(0);
        return voucherRepository.save(request);
    }

    // ── Update ────────────────────────────────────────────────────────────────────
    @Transactional
    public Voucher update(String id, Voucher request) {
        Voucher existing = findById(id);
        existing.setName(request.getName());
        existing.setDiscountType(request.getDiscountType());
        existing.setDiscountValue(request.getDiscountValue());
        existing.setMinOrderValue(request.getMinOrderValue());
        existing.setMaxUsage(request.getMaxUsage());
        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());
        existing.setStatus(request.getStatus());
        existing.setDescription(request.getDescription());
        return voucherRepository.save(existing);
    }

    // ── Delete ────────────────────────────────────────────────────────────────────
    @Transactional
    public void delete(String id) {
        findById(id);
        voucherRepository.deleteById(id);
    }

    // ── Validate (apply) ─────────────────────────────────────────────────────────
    /**
     * Kiểm tra voucher có hợp lệ không và tính giá trị giảm.
     */
    public Voucher validate(String code, BigDecimal orderValue) {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new QTHTException("exception.voucher.not_found"));
        if (!"ACTIVE".equals(voucher.getStatus()))
            throw new QTHTException("exception.voucher.inactive");
        LocalDate today = LocalDate.now();
        if (voucher.getEndDate() != null && today.isAfter(voucher.getEndDate()))
            throw new QTHTException("exception.voucher.expired");
        if (voucher.getStartDate() != null && today.isBefore(voucher.getStartDate()))
            throw new QTHTException("exception.voucher.not_started");
        if (voucher.getMinOrderValue() != null && orderValue.compareTo(voucher.getMinOrderValue()) < 0)
            throw new QTHTException("exception.voucher.min_order_not_met");
        if (voucher.getMaxUsage() != null && voucher.getUsedCount() >= voucher.getMaxUsage())
            throw new QTHTException("exception.voucher.max_usage");
        return voucher;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────
    private Voucher findById(String id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new QTHTException("exception.voucher.not_found"));
    }
}
