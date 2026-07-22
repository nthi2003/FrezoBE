package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.RegularizationAddRequest;
import com.frezo.qlns.entity.AttendanceRegularization;

import java.util.List;

public interface RegularizationService {
    AttendanceRegularization create(RegularizationAddRequest request);

    List<AttendanceRegularization> myRequests(String personId);

    List<AttendanceRegularization> pendingForManager(String managerUsername);

    AttendanceRegularization approve(String id);

    AttendanceRegularization reject(String id, String reason);
}
