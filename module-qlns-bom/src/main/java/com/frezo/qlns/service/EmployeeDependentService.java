package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.EmployeeDependentRequest;
import com.frezo.qlns.dto.response.EmployeeDependentResponse;

import java.util.List;

public interface EmployeeDependentService {
    EmployeeDependentResponse create(EmployeeDependentRequest request);
    EmployeeDependentResponse update(String id, EmployeeDependentRequest request);
    void delete(String id);
    EmployeeDependentResponse getById(String id);
    List<EmployeeDependentResponse> getByPersonId(String personId);
}
