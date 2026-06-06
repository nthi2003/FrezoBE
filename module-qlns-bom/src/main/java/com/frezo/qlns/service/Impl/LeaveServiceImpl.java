package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.qlns.entity.LeaveRecord;
import com.frezo.qlns.repository.LeaveRecordRepository;
import com.frezo.qlns.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRecordRepository leaveRepository;

    @Override
    @Transactional
    public LeaveRecord create(LeaveRecord request) {
        // ThiNVQ : Khởi tạo bản ghi nghỉ phép mới với trạng thái PENDING
        if (request.getId() == null) request.setId(UUID.randomUUID().toString());
        request.setStatus("PENDING");
        return leaveRepository.save(request);
    }

    @Override
    @Transactional
    public LeaveRecord approve(String id, String managerId) {
        // ThiNVQ : Phê duyệt nghỉ phép và cập nhật thông tin người duyệt
        LeaveRecord leave = leaveRepository.findById(id)
                .orElseThrow(() -> new QTHTException("exception.leave.not_found"));
        leave.setStatus("APPROVED");
        leave.setApprovedBy(managerId);
        return leaveRepository.save(leave);
    }

    @Override
    public List<LeaveRecord> getByPersonId(String personId) {
        // ThiNVQ : Lấy toàn bộ lịch sử nghỉ phép của một nhân viên
        return leaveRepository.findAll().stream()
                .filter(l -> l.getPersonId().equals(personId))
                .toList();
    }
}
