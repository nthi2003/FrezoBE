package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.InsuranceConfigRequest;
import com.frezo.qlns.dto.response.InsuranceConfigResponse;

public interface InsuranceConfigService {
    InsuranceConfigResponse save(InsuranceConfigRequest request);
    InsuranceConfigResponse getByYear(Integer year);
}
