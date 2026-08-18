package com.frezo.profit.service;

import com.frezo.profit.dto.request.ProfitSettingRequest;
import com.frezo.profit.dto.response.ProfitSettingDto;

import java.util.List;

public interface ProfitSettingService {

    List<ProfitSettingDto> list(String scope);

    ProfitSettingDto getById(String id);

    ProfitSettingDto create(ProfitSettingRequest req);

    ProfitSettingDto update(String id, ProfitSettingRequest req);

    void delete(String id);

    ProfitSettingDto resolveForProduct(String productId);
}
