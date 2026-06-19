package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.ContractAddRequest;
import com.frezo.qlns.dto.request.ContractAssginWorkAddRequest;
import com.frezo.qlns.dto.request.ContractEditRequest;
import com.frezo.qlns.dto.request.ContractFilter;
import com.frezo.qlns.dto.response.ContractAsginWorkResponse;
import com.frezo.qlns.dto.response.ContractComboboxResponse;
import com.frezo.qlns.dto.response.ContractResponse;
import com.frezo.util.web.Response;

import java.util.List;
import java.util.Map;

public interface ContractService {
    Response<ContractResponse> edit ( String id , ContractEditRequest request);
    Response<ContractResponse> add (ContractAddRequest request);
    Response<?> delete (String id);
    Map<String , Object> all(ContractFilter filter);
    Response<List<ContractComboboxResponse>> combobox (ContractFilter filter);

    ContractResponse view (String id);

    ContractResponse updateStatus(String id, ContractAddRequest request);

    ContractAsginWorkResponse assginWork(String contractId , ContractAssginWorkAddRequest request);

    ContractAsginWorkResponse getAssignWork(String contractId);

    ContractResponse reject(String id );

    void updateAiStatus(String id, String aiStatus);

    void updateHtmlContract(String id, String html);
}
