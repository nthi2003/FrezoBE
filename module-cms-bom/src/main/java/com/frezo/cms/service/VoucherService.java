package com.frezo.cms.service;

import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.cms.entity.Vouchers;
import com.frezo.cms.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;

    public Vouchers create(Vouchers voucher) {
        voucher.setUsedCount(0);
        return voucherRepository.save(voucher);
    }

    public Map<String, Object> getAll(int page, int size) {
        Specification<Vouchers> spec = Specification.where(GenericSpecification.hasFieldIs("isDeleted", Boolean.FALSE));
        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate");
        Page<Vouchers> pagedResult = voucherRepository.findAll(spec, ServiceHelper.createPageable(page, size, sort));
        
        return ServiceHelper.createResponse1(page, size, pagedResult, pagedResult.getContent());
    }

    public Vouchers updateStatus(String id, boolean isActive) {
        Vouchers voucher = voucherRepository.findById(id).orElseThrow(() -> new RuntimeException("Voucher not found"));
        voucher.setIsActive(isActive);
        return voucherRepository.save(voucher);
    }
}
