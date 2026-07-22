package com.frezo.qlns.listener;

import com.frezo.approval.event.ApprovalDecidedEvent;
import com.frezo.common.domain.SubjectType;
import com.frezo.qlns.entity.LeaveRequest;
import com.frezo.qlns.entity.PayrollPeriod;
import com.frezo.qlns.repository.LeaveRequestRepository;
import com.frezo.qlns.repository.PayrollPeriodRepository;
import com.frezo.qlns.service.impl.PayrollPeriodServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Đồng bộ status Leave / PayrollPeriod khi ApprovalEngine quyết định.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QlnsApprovalSubjectListener {

    private final LeaveRequestRepository leaveRequestRepository;
    private final PayrollPeriodRepository payrollPeriodRepository;

    @EventListener
    @Transactional
    public void onDecided(ApprovalDecidedEvent event) {
        if (event.getSubjectType() == null) return;
        if (SubjectType.LEAVE.name().equals(event.getSubjectType())) {
            syncLeave(event);
        } else if (SubjectType.PAYROLL.name().equals(event.getSubjectType())
                || "PAYROLL_PERIOD".equals(event.getSubjectType())) {
            syncPayroll(event);
        }
    }

    private void syncLeave(ApprovalDecidedEvent event) {
        LeaveRequest leave = leaveRequestRepository.findById(event.getSubjectId()).orElse(null);
        if (leave == null) return;
        String status = event.getStatus();
        if ("APPROVED".equals(status)) {
            leave.setStatus("APPROVED");
            leave.setHrApprovedBy(event.getActedBy());
            leave.setHrApprovedAt(LocalDate.now());
            leave.setApprovedBy(event.getActedBy());
            leave.setApprovedAt(LocalDate.now());
        } else if ("REJECTED".equals(status)) {
            leave.setStatus("REJECTED");
            leave.setRejectedBy(event.getActedBy());
            leave.setRejectedAt(LocalDate.now());
            leave.setRejectReason(event.getComment());
        } else if ("ASSIGNED".equals(status)) {
            // Bước 2 = HR
            leave.setStatus("PENDING_HR");
            leave.setManagerApprovedBy(event.getActedBy());
            leave.setManagerApprovedAt(LocalDate.now());
        }
        leaveRequestRepository.save(leave);
        log.info("[leave] Sync {} → {}", leave.getId(), leave.getStatus());
    }

    private void syncPayroll(ApprovalDecidedEvent event) {
        PayrollPeriod period = payrollPeriodRepository.findById(event.getSubjectId()).orElse(null);
        if (period == null) return;
        String status = event.getStatus();
        if ("APPROVED".equals(status)) {
            period.setStatus(PayrollPeriodServiceImpl.STATUS_CLOSED);
        } else if ("REJECTED".equals(status)) {
            period.setStatus(PayrollPeriodServiceImpl.STATUS_OPEN);
            period.setLockedAt(null);
            period.setLockedBy(null);
            period.setApprovalRequestId(null);
        } else if ("ASSIGNED".equals(status) && period.getStatus() == null) {
            period.setStatus(PayrollPeriodServiceImpl.STATUS_LOCKED);
            period.setLockedAt(LocalDateTime.now());
        }
        payrollPeriodRepository.save(period);
        log.info("[payroll] Sync {} → status {}", period.getId(), period.getStatus());
    }
}
