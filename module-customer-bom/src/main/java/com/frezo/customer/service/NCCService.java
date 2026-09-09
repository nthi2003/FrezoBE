package com.frezo.customer.service;

import com.frezo.customer.dto.request.NCCFilterRequest;
import com.frezo.customer.dto.request.NCCRequest;
import com.frezo.customer.dto.response.NCCResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface NCCService {
    Map<String, Object> all(NCCFilterRequest filter);
    NCCResponse getById(String id);
    NCCResponse createNCC(NCCRequest request);
    NCCResponse updateNCC(String id, NCCRequest request);
    void delete(String id);
    String uploadCertificate(String nccCode, MultipartFile file);
}
