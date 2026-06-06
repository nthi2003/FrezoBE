package com.frezo.customer.service;

import com.frezo.customer.dto.request.CustomerFilterRequest;
import com.frezo.customer.dto.request.CustomerRequest;
import com.frezo.customer.dto.response.CustomerResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface CustomerService {

    Map<String, Object> getAll(CustomerFilterRequest filter);

    CustomerResponse getById(String id);

    CustomerResponse create(CustomerRequest request);

    CustomerResponse update(String id, CustomerRequest request);

    void delete(String id);

    // Xuất SĐT thật – yêu cầu role cao / audit log
    String revealPhone(String id);

    // Import từ Excel
    void importFromExcel(MultipartFile file);

    // Export ra Excel
    byte[] exportToExcel(CustomerFilterRequest filter);

    // Đồng bộ khách hàng tiềm năng từ AI Scraper
    int syncLeadsFromAi(String keyword, String city, String ward, Integer limit);
}
