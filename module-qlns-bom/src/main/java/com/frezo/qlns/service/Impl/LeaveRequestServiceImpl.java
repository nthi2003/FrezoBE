package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.response.PageResponse;
import com.frezo.common.service.NotificationService;
import com.frezo.qlns.dto.request.LeaveRequestAddRequest;
import com.frezo.qlns.dto.response.LeaveRequestResponse;
import com.frezo.qlns.entity.LeaveRequest;
import com.frezo.qlns.mapper.LeaveRequestMapper;
import com.frezo.qlns.repository.LeaveRequestRepository;
import com.frezo.qlns.service.LeaveRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LeaveRequestServiceImpl implements LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveRequestMapper leaveRequestMapper;
    private final NotificationService notificationService;

    @Override
    public LeaveRequestResponse create(LeaveRequestAddRequest request) {
        LeaveRequest entity = leaveRequestMapper.toEntity(request);
        entity.setStatus("PENDING_MANAGER"); // First level
        LeaveRequest saved = leaveRequestRepository.save(entity);
        
        // Notify Manager (mocking manager name for now)
        notificationService.notifyUserWithEmailFallback("manager_user", 
            "New Leave Request", 
            "User " + entity.getPersonId() + " has requested leave.", 
            false);
            
        return leaveRequestMapper.toResponse(saved);
    }

    @Override
    public LeaveRequestResponse approve(String id) {
        LeaveRequest entity = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new QTHTException("error.leave.request.not.found"));
        
        String currentStatus = entity.getStatus();
        String nextStatus;
        
        if ("PENDING_MANAGER".equals(currentStatus)) {
            nextStatus = "PENDING_HR";
            notificationService.notifyUserWithEmailFallback("hr_user", 
                "Leave Request Pending HR Approval", 
                "A leave request has been approved by the manager and needs HR review.", 
                false);
        } else if ("PENDING_HR".equals(currentStatus)) {
            nextStatus = "APPROVED";
            notificationService.notifyUserWithEmailFallback(entity.getCreatedBy(), 
                "Leave Request Approved", 
                "Your leave request has been fully approved.", 
                true);
        } else {
            throw new QTHTException("error.leave.request.invalid.status");
        }

        entity.setStatus(nextStatus);
        entity.setApprovedBy(SystemUtils.getCurrentUsername());
        entity.setApprovedAt(LocalDate.now());
        LeaveRequest saved = leaveRequestRepository.save(entity);
        return leaveRequestMapper.toResponse(saved);
    }

    @Override
    public LeaveRequestResponse reject(String id, String reason) {
        LeaveRequest entity = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new QTHTException("error.leave.request.not.found"));
        entity.setStatus("REJECTED");
        entity.setRejectedBy(SystemUtils.getCurrentUsername());
        entity.setRejectReason(reason);
        LeaveRequest saved = leaveRequestRepository.save(entity);
        
        // Notify Requester
        notificationService.notifyUserWithEmailFallback(entity.getCreatedBy(), 
            "Leave Request Rejected", 
            "Your leave request was rejected. Reason: " + reason, 
            true);
            
        return leaveRequestMapper.toResponse(saved);
    }

    @Override
    public LeaveRequestResponse cancel(String id) {
        LeaveRequest entity = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new QTHTException("error.leave.request.not.found"));
        entity.setStatus("CANCELLED");
        LeaveRequest saved = leaveRequestRepository.save(entity);
        return leaveRequestMapper.toResponse(saved);
    }

    @Override
    public List<LeaveRequestResponse> getMyRequests(String contractId) {
        List<LeaveRequest> requests = leaveRequestRepository.findByContractIdAndStatus(contractId, "PENDING"); // Or get all status depending on requirement
        return requests.stream().map(leaveRequestMapper::toResponse).toList();
    }

    @Override
    public Map<String, Object> allPending(int page, int size) {
        Specification<LeaveRequest> spec = Specification.where(GenericSpecification.equalField("status", "PENDING"));
        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate");
        Page<LeaveRequest> pagedResult = leaveRequestRepository.findAll(spec, ServiceHelper.createPageable(page, size, sort));

        List<LeaveRequestResponse> responses = pagedResult.getContent().stream().map(leaveRequestMapper::toResponse).toList();
        return Map.of(
            "pageNumber", page,
            "pageSize", size,
            "total", pagedResult.getTotalElements(),
            "items", responses
        );
    }
}
