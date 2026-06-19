package com.frezo.qlns.service.Impl;

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

    @Override
    @Transactional
    public TaxConfigResponse save(TaxConfigRequest request) {
        TaxConfig entity = taxConfigMapper.toEntity(request);
        return taxConfigMapper.toResponse(taxConfigRepository.save(entity));
    }

    @Override
    public List<TaxConfigResponse> getByYear(Integer year) {
        return taxConfigRepository.findByYearAndIsActiveTrueOrderByBracketOrderAsc(year)
                .stream().map(taxConfigMapper::toResponse).toList();
    }
}
