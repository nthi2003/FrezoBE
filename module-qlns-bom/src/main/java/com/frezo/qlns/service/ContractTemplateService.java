package com.frezo.qlns.service;

import com.frezo.qlns.dto.response.ContractTemplateResponse;

import java.util.List;

public interface ContractTemplateService {
    List<ContractTemplateResponse> getAll();
    ContractTemplateResponse create(String name, String type, String fileUrl, String fileObjectName);
    void delete(String id);
}
