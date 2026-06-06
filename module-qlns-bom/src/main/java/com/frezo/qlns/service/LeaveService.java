package com.frezo.qlns.service;

import com.frezo.qlns.entity.LeaveRecord;
import java.util.List;

public interface LeaveService {
    LeaveRecord create(LeaveRecord request);
    LeaveRecord approve(String id, String managerId);
    List<LeaveRecord> getByPersonId(String personId);
}
