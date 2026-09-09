package com.frezo.qlns.service.impl;

import com.frezo.qlns.dto.request.TaxConfigRequest;
import com.frezo.qlns.dto.response.TaxConfigResponse;
import com.frezo.qlns.entity.TaxConfig;
import com.frezo.qlns.mapper.TaxConfigMapper;
import com.frezo.qlns.repository.TaxConfigRepository;
import com.frezo.qlns.service.TaxConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxConfigServiceImpl implements TaxConfigService {

    private final TaxConfigRepository taxConfigRepository;
    private final TaxConfigMapper taxConfigMapper;

    /**
     * Lưu mới hoặc cập nhật cấu hình thuế TNCN theo bậc thuế vào hệ thống.
     */
    @Override
    @Transactional
    public TaxConfigResponse save(TaxConfigRequest request) {
        TaxConfig entity = taxConfigMapper.toEntity(request);
        return taxConfigMapper.toResponse(taxConfigRepository.save(entity));
    }

    /**
     * Lấy danh sách cấu hình thuế đang hoạt động của một năm, sắp xếp theo thứ tự bậc thuế.
     */
    @Override
    public List<TaxConfigResponse> getByYear(Integer year) {
        return taxConfigRepository.findByYearAndIsActiveTrueOrderByBracketOrderAsc(year)
                .stream().map(taxConfigMapper::toResponse).toList();
    }
}
