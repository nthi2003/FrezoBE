package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.helper.SystemUtils;
import com.frezo.qlns.dto.request.OnboardingAssignRequest;
import com.frezo.qlns.dto.request.OnboardingTemplateItemRequest;
import com.frezo.qlns.dto.request.OnboardingTemplateRequest;
import com.frezo.qlns.dto.response.OnboardingAssignmentItemResponse;
import com.frezo.qlns.dto.response.OnboardingAssignmentResponse;
import com.frezo.qlns.dto.response.OnboardingTemplateItemResponse;
import com.frezo.qlns.dto.response.OnboardingTemplateResponse;
import com.frezo.qlns.entity.OnboardingAssignment;
import com.frezo.qlns.entity.OnboardingAssignmentItem;
import com.frezo.qlns.entity.OnboardingTemplate;
import com.frezo.qlns.entity.OnboardingTemplateItem;
import com.frezo.qlns.repository.OnboardingAssignmentItemRepository;
import com.frezo.qlns.repository.OnboardingAssignmentRepository;
import com.frezo.qlns.repository.OnboardingTemplateItemRepository;
import com.frezo.qlns.repository.OnboardingTemplateRepository;
import com.frezo.qlns.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Template CRUD tách thin — assignment dùng cùng class nhưng chỉ 4 repo deps.
 */
@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final OnboardingTemplateRepository templateRepository;
    private final OnboardingTemplateItemRepository templateItemRepository;
    private final OnboardingAssignmentRepository assignmentRepository;
    private final OnboardingAssignmentItemRepository assignmentItemRepository;

    @Override
    public List<OnboardingTemplateResponse> listTemplates() {
        return templateRepository.findByIsDeletedFalseOrderByCreatedDateDesc().stream()
                .map(this::toTemplateDto).toList();
    }

    @Override
    @Transactional
    public OnboardingTemplateResponse createTemplate(OnboardingTemplateRequest req) {
        OnboardingTemplate t = OnboardingTemplate.builder()
                .name(req.getName())
                .description(req.getDescription())
                .active(req.getActive() == null || req.getActive())
                .build();
        t.setId(UUID.randomUUID().toString());
        t = templateRepository.save(t);
        saveTemplateItems(t.getId(), req.getItems());
        return toTemplateDto(t);
    }

    @Override
    @Transactional
    public OnboardingTemplateResponse updateTemplate(String id, OnboardingTemplateRequest req) {
        OnboardingTemplate t = templateRepository.findById(id)
                .filter(x -> Boolean.FALSE.equals(x.getIsDeleted()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Template không tồn tại"));
        if (req.getName() != null) t.setName(req.getName());
        if (req.getDescription() != null) t.setDescription(req.getDescription());
        if (req.getActive() != null) t.setActive(req.getActive());
        templateRepository.save(t);
        if (req.getItems() != null) {
            templateItemRepository.findByTemplateIdAndIsDeletedFalseOrderBySortOrderAsc(id)
                    .forEach(i -> { i.setIsDeleted(true); templateItemRepository.save(i); });
            saveTemplateItems(id, req.getItems());
        }
        return toTemplateDto(t);
    }

    @Override
    public List<OnboardingAssignmentResponse> listAssignments(String personId) {
        List<OnboardingAssignment> list = personId != null
                ? assignmentRepository.findByPersonIdAndIsDeletedFalse(personId)
                : assignmentRepository.findByIsDeletedFalseOrderByCreatedDateDesc();
        return list.stream().map(this::toAssignmentDto).toList();
    }

    @Override
    @Transactional
    public OnboardingAssignmentResponse assign(OnboardingAssignRequest req) {
        OnboardingTemplate t = templateRepository.findById(req.getTemplateId())
                .filter(x -> Boolean.FALSE.equals(x.getIsDeleted()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Template không tồn tại"));
        LocalDate start = req.getStartDate() != null ? req.getStartDate() : LocalDate.now();
        OnboardingAssignment a = OnboardingAssignment.builder()
                .templateId(t.getId())
                .personId(req.getPersonId())
                .startDate(start)
                .status("IN_PROGRESS")
                .progress(0.0)
                .build();
        a.setId(UUID.randomUUID().toString());
        a = assignmentRepository.save(a);

        List<OnboardingTemplateItem> items = templateItemRepository
                .findByTemplateIdAndIsDeletedFalseOrderBySortOrderAsc(t.getId());
        int order = 0;
        for (OnboardingTemplateItem ti : items) {
            int offset = ti.getDueDayOffset() != null ? ti.getDueDayOffset() : 0;
            OnboardingAssignmentItem ai = OnboardingAssignmentItem.builder()
                    .assignmentId(a.getId())
                    .templateItemId(ti.getId())
                    .title(ti.getTitle())
                    .dueDate(start.plusDays(offset))
                    .status("PENDING")
                    .sortOrder(ti.getSortOrder() != null ? ti.getSortOrder() : order++)
                    .build();
            ai.setId(UUID.randomUUID().toString());
            assignmentItemRepository.save(ai);
        }
        return toAssignmentDto(a);
    }

    @Override
    @Transactional
    public OnboardingAssignmentResponse completeItem(String assignmentId, String itemId) {
        OnboardingAssignment a = assignmentRepository.findById(assignmentId)
                .filter(x -> Boolean.FALSE.equals(x.getIsDeleted()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Assignment không tồn tại"));
        OnboardingAssignmentItem item = assignmentItemRepository.findById(itemId)
                .filter(i -> assignmentId.equals(i.getAssignmentId()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Item không tồn tại"));
        item.setStatus("DONE");
        item.setCompletedAt(LocalDateTime.now());
        item.setCompletedBy(SystemUtils.getCurrentUsername());
        assignmentItemRepository.save(item);

        List<OnboardingAssignmentItem> all = assignmentItemRepository
                .findByAssignmentIdAndIsDeletedFalseOrderBySortOrderAsc(assignmentId);
        long done = all.stream().filter(i -> "DONE".equals(i.getStatus()) || "SKIPPED".equals(i.getStatus())).count();
        double progress = all.isEmpty() ? 100.0 : Math.round((done * 1000.0) / all.size()) / 10.0;
        a.setProgress(progress);
        if (done == all.size()) a.setStatus("COMPLETED");
        assignmentRepository.save(a);
        return toAssignmentDto(a);
    }

    private void saveTemplateItems(String templateId, List<OnboardingTemplateItemRequest> items) {
        if (items == null) return;
        int order = 0;
        for (OnboardingTemplateItemRequest r : items) {
            OnboardingTemplateItem i = OnboardingTemplateItem.builder()
                    .templateId(templateId)
                    .title(r.getTitle())
                    .description(r.getDescription())
                    .assigneeRole(r.getAssigneeRole())
                    .dueDayOffset(r.getDueDayOffset() != null ? r.getDueDayOffset() : 0)
                    .sortOrder(r.getSortOrder() != null ? r.getSortOrder() : order++)
                    .required(r.getRequired() == null || r.getRequired())
                    .build();
            i.setId(UUID.randomUUID().toString());
            templateItemRepository.save(i);
        }
    }

    private OnboardingTemplateResponse toTemplateDto(OnboardingTemplate t) {
        List<OnboardingTemplateItemResponse> items = templateItemRepository
                .findByTemplateIdAndIsDeletedFalseOrderBySortOrderAsc(t.getId())
                .stream()
                .map(i -> OnboardingTemplateItemResponse.builder()
                        .id(i.getId()).title(i.getTitle()).description(i.getDescription())
                        .assigneeRole(i.getAssigneeRole()).dueDayOffset(i.getDueDayOffset())
                        .sortOrder(i.getSortOrder()).required(i.getRequired()).build())
                .toList();
        return OnboardingTemplateResponse.builder()
                .id(t.getId()).name(t.getName()).description(t.getDescription())
                .active(t.getActive()).items(items).build();
    }

    private OnboardingAssignmentResponse toAssignmentDto(OnboardingAssignment a) {
        List<OnboardingAssignmentItemResponse> items = assignmentItemRepository
                .findByAssignmentIdAndIsDeletedFalseOrderBySortOrderAsc(a.getId())
                .stream()
                .map(i -> OnboardingAssignmentItemResponse.builder()
                        .id(i.getId()).title(i.getTitle()).dueDate(i.getDueDate())
                        .status(i.getStatus())
                        .completedAt(i.getCompletedAt() != null ? i.getCompletedAt().format(ISO) : null)
                        .completedBy(i.getCompletedBy()).sortOrder(i.getSortOrder()).build())
                .toList();
        return OnboardingAssignmentResponse.builder()
                .id(a.getId()).templateId(a.getTemplateId()).personId(a.getPersonId())
                .startDate(a.getStartDate()).status(a.getStatus()).progress(a.getProgress())
                .items(items).build();
    }
}
