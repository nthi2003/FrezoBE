package com.frezo.crm.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.crm.common.CrmErrorCode;
import com.frezo.crm.common.DealStatus;
import com.frezo.crm.common.LeadStatus;
import com.frezo.crm.dto.LeadRequest;
import com.frezo.crm.entity.Deal;
import com.frezo.crm.entity.Lead;
import com.frezo.crm.entity.Pipeline;
import com.frezo.crm.entity.Stage;
import com.frezo.crm.repository.DealRepository;
import com.frezo.crm.repository.LeadRepository;
import com.frezo.crm.service.LeadService;
import com.frezo.crm.service.PipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepo;
    private final DealRepository dealRepo;
    private final PipelineService pipelineService;

    /**
     * Tạo mới một lead (khách hàng tiềm năng) từ thông tin trong LeadRequest.
     *
     * @param r yêu cầu chứa thông tin lead
     * @return lead vừa được tạo
     */
    @Override
    @Transactional
    public Lead create(LeadRequest r) {
        Lead l = Lead.builder()
                .fullName(r.getFullName())
                .phone(r.getPhone())
                .email(r.getEmail())
                .companyName(r.getCompanyName())
                .source(r.getSource())
                .status(r.getStatus() != null ? r.getStatus() : LeadStatus.NEW)
                .score(r.getScore())
                .ownerUsername(r.getOwnerUsername())
                .description(r.getDescription())
                .build();
        l.setIsDeleted(false);
        return leadRepo.save(l);
    }

    /**
     * Cập nhật thông tin lead theo mã định danh.
     *
     * @param id mã lead
     * @param r yêu cầu cập nhật
     * @return lead đã cập nhật
     */
    @Override
    @Transactional
    public Lead update(String id, LeadRequest r) {
        Lead l = get(id);
        l.setFullName(r.getFullName());
        l.setPhone(r.getPhone());
        l.setEmail(r.getEmail());
        l.setCompanyName(r.getCompanyName());
        l.setSource(r.getSource());
        if (r.getStatus() != null) l.setStatus(r.getStatus());
        l.setScore(r.getScore());
        l.setOwnerUsername(r.getOwnerUsername());
        l.setDescription(r.getDescription());
        return leadRepo.save(l);
    }

    /** Xóa mềm lead theo mã định danh. */
    @Override
    @Transactional
    public void delete(String id) {
        Lead l = get(id);
        l.setIsDeleted(true);
        leadRepo.save(l);
    }

    /** Lấy thông tin lead theo mã định danh, ném ngoại lệ nếu không tìm thấy. */
    @Override
    @Transactional(readOnly = true)
    public Lead get(String id) {
        return leadRepo.findById(id)
                .orElseThrow(() -> new AppException(CrmErrorCode.LEAD_NOT_FOUND, id));
    }

    /** Lấy danh sách tất cả lead đang hoạt động, sắp xếp theo ngày tạo giảm dần. */
    @Override
    @Transactional(readOnly = true)
    public List<Lead> list() {
        return leadRepo.findByIsDeletedFalseOrderByCreatedDateDesc();
    }

    /** Lấy danh sách lead theo trạng thái, sắp xếp theo ngày tạo giảm dần. */
    @Override
    @Transactional(readOnly = true)
    public List<Lead> byStatus(LeadStatus status) {
        return leadRepo.findByStatusAndIsDeletedFalseOrderByCreatedDateDesc(status);
    }

    /** Lấy danh sách lead theo chủ sở hữu, sắp xếp theo ngày tạo giảm dần. */
    @Override
    @Transactional(readOnly = true)
    public List<Lead> byOwner(String owner) {
        return leadRepo.findByOwnerUsernameAndIsDeletedFalseOrderByCreatedDateDesc(owner);
    }

    /**
     * Chuyển đổi lead thành deal (thương vụ).
     * Tạo deal mới trong pipeline/stage được chỉ định, đánh dấu lead là đã chuyển đổi.
     *
     * @param leadId mã lead cần chuyển
     * @param pipelineId mã pipeline (có thể null để dùng mặc định)
     * @param stageId mã stage (có thể null để dùng stage đầu tiên)
     * @param customerId mã khách hàng
     * @param amount giá trị deal
     * @return mã deal vừa được tạo
     */
    @Override
    @Transactional
    public String convert(String leadId, String pipelineId, String stageId,
                          String customerId, BigDecimal amount) {
        Lead lead = get(leadId);
        if (lead.getStatus() == LeadStatus.CONVERTED) {
            throw new AppException(CrmErrorCode.LEAD_ALREADY_CONVERTED, leadId);
        }
        Pipeline pipeline = pipelineId != null
                ? pipelineService.get(pipelineId)
                : pipelineService.ensureDefault();
        Stage stage = pipelineService.stages(pipeline.getId()).stream()
                .filter(s -> stageId == null || s.getId().equals(stageId))
                .findFirst()
                .orElseThrow(() -> new AppException(CrmErrorCode.STAGE_NOT_FOUND, stageId));

        Deal deal = Deal.builder()
                .title(lead.getFullName()
                        + (lead.getCompanyName() != null ? " – " + lead.getCompanyName() : ""))
                .pipelineId(pipeline.getId())
                .stageId(stage.getId())
                .customerId(customerId)
                .amount(amount)
                .currency("VND")
                .probability(stage.getProbability())
                .status(DealStatus.OPEN)
                .ownerUsername(lead.getOwnerUsername())
                .description("Từ Lead " + lead.getId())
                .build();
        deal.setIsDeleted(false);
        dealRepo.save(deal);

        lead.setStatus(LeadStatus.CONVERTED);
        lead.setConvertedCustomerId(customerId);
        lead.setConvertedDealId(deal.getId());
        leadRepo.save(lead);

        return deal.getId();
    }
}
