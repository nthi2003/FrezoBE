package com.frezo.qtbv.service;

import com.frezo.qtbv.dto.response.ManagerResponse;

import java.util.List;

public interface ManagerService {
    List<ManagerResponse> getManagersList();
}
