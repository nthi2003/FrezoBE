package com.frezo.qtht.service;

import com.frezo.qtht.dto.request.IpTrustAddRequest;
import com.frezo.qtht.dto.request.IpTrustEditRequest;
import com.frezo.qtht.dto.request.IpTrustFilter;
import com.frezo.qtht.dto.response.IpTrustResponse;
import com.frezo.util.web.Response;

import java.util.Map;

public interface IPTrustService {
    Map<String, Object> all (IpTrustFilter filter);
    Response<IpTrustResponse> add(IpTrustAddRequest request);
    Response<IpTrustResponse> edit(String id, IpTrustEditRequest request);
    IpTrustResponse view (String id);
    Response<?> delete(String id);
}
