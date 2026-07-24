package com.frezo.qtht.service;

import com.frezo.qtht.dto.request.IpTrustAddRequest;
import com.frezo.qtht.dto.request.IpTrustEditRequest;
import com.frezo.qtht.dto.request.IpTrustFilter;
import com.frezo.qtht.dto.response.IpTrustResponse;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.PageResponse;

public interface IPTrustService {
    PageResponse<IpTrustResponse> all(IpTrustFilter filter);
    ApiResponse<IpTrustResponse> add(IpTrustAddRequest request);
    ApiResponse<IpTrustResponse> edit(String id, IpTrustEditRequest request);
    IpTrustResponse view (String id);
    ApiResponse<?> delete(String id);
}
