package com.frezo.customer.service;

import com.frezo.common.response.PageResponse;
import com.frezo.customer.dto.request.CustomerFilterRequest;
import com.frezo.customer.dto.request.CustomerRequest;
import com.frezo.customer.dto.response.CustomerResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CustomerService {

    PageResponse<CustomerResponse> getAll(CustomerFilterRequest filter);

    CustomerResponse getById(String id);

    CustomerResponse create(CustomerRequest request);

    CustomerResponse update(String id, CustomerRequest request);

    void delete(String id);

    String revealPhone(String id);

    void importFromExcel(MultipartFile file);


    byte[] exportToExcel(CustomerFilterRequest filter);

    int syncLeadsFromAi(String keyword, String city, String ward, Integer limit);


    String uploadAvatar(String id, MultipartFile file);
}
