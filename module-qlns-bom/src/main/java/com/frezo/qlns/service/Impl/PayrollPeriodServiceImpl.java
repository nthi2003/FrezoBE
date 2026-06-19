package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.response.PageResponse;
import com.frezo.qlns.dto.request.PayrollPeriodRequest;
import com.frezo.qlns.dto.response.PayrollPeriodResponse;
import com.frezo.qlns.entity.PayrollPeriod;
import com.frezo.qlns.mapper.PayrollPeriodMapper;
import com.frezo.qlns.repository.PayrollPeriodRepository;
import com.frezo.qlns.service.PayrollPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PayrollPeriodServiceImpl implements PayrollPeriodService {

    private final PayrollPeriodRepository payrollPeriodRepository;
    private final PayrollPeriodMapper payrollPeriodMapper;

    @Override
    @Transactional
    public PayrollPeriodResponse create(PayrollPeriodRequest request) {
        if (payrollPeriodRepository.findByOrgIdAndMonthAndYear(request.getOrgId(), request.getMonth(), request.getYear()).isPresent()) {
            throw new QTHTException("Kỳ lương đã tồn tại cho tháng " + request.getMonth() + "/" + request.getYear());
        }
        PayrollPeriod entity = PayrollPeriod.builder()
                .orgId(request.getOrgId())
                .month(request.getMonth())
                .year(request.getYear())
                .name(request.getName())
                .status(0)
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .paymentDate(request.getPaymentDate())
                .note(request.getNote())
                .build();
        return payrollPeriodMapper.toResponse(payrollPeriodRepository.save(entity));
    }

    @Override
    @Transactional
    public PayrollPeriodResponse update(String id, PayrollPeriodRequest request) {
        PayrollPeriod entity = payrollPeriodRepository.findById(id)
                .orElseThrow(() -> new QTHTException("Không tìm thấy kỳ lương"));
        if (entity.getStatus() == 1 || entity.getStatus() == 2) {
            throw new QTHTException("Không thể sửa kỳ lương đã khóa");
        }
        entity.setName(request.getName());
        entity.setFromDate(request.getFromDate());
        entity.setToDate(request.getToDate());
        entity.setPaymentDate(request.getPaymentDate());
        entity.setNote(request.getNote());
        return payrollPeriodMapper.toResponse(payrollPeriodRepository.save(entity));
    }

    @Override
    @Transactional
    public PayrollPeriodResponse lock(String id) {
        PayrollPeriod entity = payrollPeriodRepository.findById(id)
                .orElseThrow(() -> new QTHTException("Không tìm thấy kỳ lương"));
        if (entity.getStatus() == 1 || entity.getStatus() == 2) {
            throw new QTHTException("Kỳ lương đã được khóa");
        }
        entity.setStatus(1);
        entity.setLockedAt(LocalDateTime.now());
        return payrollPeriodMapper.toResponse(payrollPeriodRepository.save(entity));
    }

    @Override
    @Transactional
    public PayrollPeriodResponse unlock(String id) {
        PayrollPeriod entity = payrollPeriodRepository.findById(id)
                .orElseThrow(() -> new QTHTException("Không tìm thấy kỳ lương"));
        if (entity.getStatus() == 2) {
            throw new QTHTException("Không thể mở khóa kỳ lương đã đóng");
        }
        entity.setStatus(0);
        entity.setLockedAt(null);
        return payrollPeriodMapper.toResponse(payrollPeriodRepository.save(entity));
    }

    @Override
    @Transactional
    public PayrollPeriodResponse close(String id) {
        PayrollPeriod entity = payrollPeriodRepository.findById(id)
                .orElseThrow(() -> new QTHTException("Không tìm thấy kỳ lương"));
        entity.setStatus(2);
        return payrollPeriodMapper.toResponse(payrollPeriodRepository.save(entity));
    }

    @Override
    public PayrollPeriodResponse getById(String id) {
        return payrollPeriodRepository.findById(id)
                .map(payrollPeriodMapper::toResponse)
                .orElseThrow(() -> new QTHTException("Không tìm thấy kỳ lương"));
    }

    @Override
    public PageResponse<PayrollPeriodResponse> getAll(String orgId, Integer month, Integer year, Integer status,
                                                       Integer pageNumber, Integer pageSize) {
        Specification<PayrollPeriod> spec = Specification.where(GenericSpecification.equalField("isDeleted", false));
        if (orgId != null) spec = spec.and(GenericSpecification.equalField("orgId", orgId));
        if (month != null) spec = spec.and(GenericSpecification.equalField("month", month));
        if (year != null) spec = spec.and(GenericSpecification.equalField("year", year));
        if (status != null) spec = spec.and(GenericSpecification.equalField("status", status));

        Sort sort = Sort.by(Sort.Direction.DESC, "year").and(Sort.by(Sort.Direction.DESC, "month"));
        Page<PayrollPeriod> pageResult = payrollPeriodRepository.findAll(spec,
                ServiceHelper.createPageable(pageNumber, pageSize, sort));
        var items = pageResult.getContent().stream().map(payrollPeriodMapper::toResponse).toList();
        return PageResponse.of(pageNumber, pageSize, pageResult, items);
    }
}
