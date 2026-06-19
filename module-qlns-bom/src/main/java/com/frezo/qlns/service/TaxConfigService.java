package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.TaxConfigRequest;
import com.frezo.qlns.dto.response.TaxConfigResponse;

import java.util.List;

public interface TaxConfigService {
    TaxConfigResponse save(TaxConfigRequest request);
    List<TaxConfigResponse> getByYear(Integer year);
}
