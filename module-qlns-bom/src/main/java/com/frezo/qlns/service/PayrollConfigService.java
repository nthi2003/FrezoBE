package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.PayrollConfigRequest;
import com.frezo.qlns.dto.response.PayrollConfigResponse;

public interface PayrollConfigService {
    PayrollConfigResponse save(PayrollConfigRequest request);
    PayrollConfigResponse getByOrgAndYear(String orgId, Integer year);
}
