package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.ContractAddRequest;
import com.frezo.qlns.dto.request.ContractAssginWorkAddRequest;
import com.frezo.qlns.dto.request.ContractEditRequest;
import com.frezo.qlns.dto.request.ContractFilter;
import com.frezo.qlns.dto.response.ContractAsginWorkResponse;
import com.frezo.qlns.dto.response.ContractComboboxResponse;
import com.frezo.qlns.dto.response.ContractResponse;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.PageResponse;

import java.util.List;

public interface ContractService {
    ApiResponse<ContractResponse> edit ( String id , ContractEditRequest request);
    ApiResponse<ContractResponse> add (ContractAddRequest request);
    ApiResponse<?> delete (String id);
    PageResponse<ContractResponse> all(ContractFilter filter);
    ApiResponse<List<ContractComboboxResponse>> combobox (ContractFilter filter);

    ContractResponse view (String id);

    ContractResponse updateStatus(String id, ContractAddRequest request);

    ContractAsginWorkResponse assginWork(String contractId , ContractAssginWorkAddRequest request);

    ContractAsginWorkResponse getAssignWork(String contractId);

    ContractResponse reject(String id );

    void updateAiStatus(String id, String aiStatus);

    void updateHtmlContract(String id, String html);
}
