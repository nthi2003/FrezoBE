package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.helper.SystemUtils;
import com.frezo.qlns.dto.request.OkrCheckInRequest;
import com.frezo.qlns.dto.request.OkrKeyResultRequest;
import com.frezo.qlns.dto.request.OkrRequest;
import com.frezo.qlns.dto.response.OkrKeyResultResponse;
import com.frezo.qlns.dto.response.OkrListResponse;
import com.frezo.qlns.dto.response.OkrResponse;
import com.frezo.qlns.dto.response.OkrViewerContext;
import com.frezo.qlns.entity.Okr;
import com.frezo.qlns.entity.OkrKeyResult;
import com.frezo.qlns.repository.OkrKeyResultRepository;
import com.frezo.qlns.repository.OkrRepository;
import com.frezo.qlns.service.OkrService;
import com.frezo.qlns.service.impl.OkrScopeResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OkrServiceImpl implements OkrService {

    private final OkrRepository okrRepository;
    private final OkrKeyResultRepository keyResultRepository;
    private final OkrScopeResolver scopeResolver;

    @Override
    public OkrListResponse list(String scope, String ownerPersonId) {
        String me = scopeResolver.currentPersonId()
                .orElseThrow(() -> new AppException(CommonErrorCode.FORBIDDEN, "Tài khoản chưa liên kết nhân sự"));

        String scopeNorm = OkrScopeResolver.normalizeScope(scope);
        scopeResolver.assertScopeAllowed(scopeNorm, me);

        boolean admin = scopeResolver.isAdmin();
        boolean manager = scopeResolver.isManager(me);

        List<Okr> list = switch (scopeNorm) {
            case "all" -> listAll(ownerPersonId);
            case "team" -> listTeam(me, admin, ownerPersonId);
            default -> okrRepository.findByOwnerPersonIdAndIsDeletedFalse(me);
        };

        return OkrListResponse.builder()
                .items(list.stream().map(this::toResponse).toList())
                .viewer(buildViewer(me, admin, manager))
                .build();
    }

    private List<Okr> listAll(String ownerPersonId) {
        if (ownerPersonId != null && !ownerPersonId.isBlank()) {
            return okrRepository.findByOwnerPersonIdAndIsDeletedFalse(ownerPersonId);
        }
        return okrRepository.findByIsDeletedFalseOrderByCreatedDateDesc();
    }

    private List<Okr> listTeam(String me, boolean admin, String ownerPersonId) {
        List<String> subIds = scopeResolver.subordinatePersonIds(me);
        if (ownerPersonId != null && !ownerPersonId.isBlank()) {
            if (!admin && !subIds.contains(ownerPersonId)) {
                throw new AppException(CommonErrorCode.FORBIDDEN, "Nhân viên không thuộc team của bạn");
            }
            return okrRepository.findByOwnerPersonIdAndIsDeletedFalse(ownerPersonId);
        }
        if (subIds.isEmpty()) return List.of();
        return okrRepository.findByOwnerPersonIdInAndIsDeletedFalseOrderByCreatedDateDesc(subIds);
    }

    private OkrViewerContext buildViewer(String personId, boolean admin, boolean manager) {
        List<String> scopes = new ArrayList<>();
        scopes.add("mine");
        if (manager || admin) scopes.add("team");
        if (admin) scopes.add("all");
        return OkrViewerContext.builder()
                .personId(personId)
                .admin(admin)
                .manager(manager)
                .allowedScopes(scopes)
                .build();
    }

    @Override
    public OkrResponse get(String id) {
        Okr okr = find(id);
        scopeResolver.assertCanView(okr.getOwnerPersonId());
        return toResponse(okr);
    }

    @Override
    @Transactional
    public OkrResponse create(OkrRequest req) {
        validateRequest(req, true);
        String me = scopeResolver.currentPersonId()
                .orElseThrow(() -> new AppException(CommonErrorCode.FORBIDDEN, "Tài khoản chưa liên kết nhân sự"));

        String ownerId = (req.getOwnerPersonId() != null && !req.getOwnerPersonId().isBlank())
                ? req.getOwnerPersonId()
                : me;
        scopeResolver.assertCanAssignOwner(ownerId);

        Okr okr = Okr.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .ownerPersonId(ownerId)
                .periodLabel(req.getPeriodLabel())
                .cycleId(req.getCycleId())
                .departmentId(req.getDepartmentId())
                .orgId(req.getOrgId())
                .parentOkrId(req.getParentOkrId())
                .scopeType(normalizeScopeType(req.getScopeType()))
                .objectiveType(normalizeObjectiveType(req.getObjectiveType()))
                .crossLinkIds(join(req.getCrossLinkIds()))
                .published(false)
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .status(req.getStatus() != null ? req.getStatus() : "ACTIVE")
                .progress(0.0)
                .build();
        okr.setId(UUID.randomUUID().toString());
        okr = okrRepository.save(okr);
        saveKeyResults(okr.getId(), req.getKeyResults(), true);
        recalculateProgress(okr);
        if (Boolean.TRUE.equals(req.getPublished())) {
            publishInternal(okr);
        }
        return toResponse(okr);
    }

    @Override
    @Transactional
    public OkrResponse update(String id, OkrRequest req) {
        Okr okr = find(id);
        scopeResolver.assertCanModify(okr.getOwnerPersonId());
        validateRequest(req, false);
        if (req.getTitle() != null) okr.setTitle(req.getTitle());
        if (req.getDescription() != null) okr.setDescription(req.getDescription());
        if (req.getOwnerPersonId() != null) {
            scopeResolver.assertCanAssignOwner(req.getOwnerPersonId());
            okr.setOwnerPersonId(req.getOwnerPersonId());
        }
        if (req.getPeriodLabel() != null) okr.setPeriodLabel(req.getPeriodLabel());
        if (req.getCycleId() != null) okr.setCycleId(req.getCycleId());
        if (req.getDepartmentId() != null) okr.setDepartmentId(req.getDepartmentId());
        if (req.getOrgId() != null) okr.setOrgId(req.getOrgId());
        if (req.getParentOkrId() != null) okr.setParentOkrId(req.getParentOkrId());
        if (req.getScopeType() != null) okr.setScopeType(normalizeScopeType(req.getScopeType()));
        if (req.getObjectiveType() != null) okr.setObjectiveType(normalizeObjectiveType(req.getObjectiveType()));
        if (req.getCrossLinkIds() != null) okr.setCrossLinkIds(join(req.getCrossLinkIds()));
        if (req.getStartDate() != null) okr.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) okr.setEndDate(req.getEndDate());
        if (req.getStatus() != null) okr.setStatus(req.getStatus());
        okrRepository.save(okr);
        if (req.getKeyResults() != null) {
            keyResultRepository.findByOkrIdAndIsDeletedFalseOrderBySortOrderAsc(id)
                    .forEach(kr -> { kr.setIsDeleted(true); keyResultRepository.save(kr); });
            saveKeyResults(id, req.getKeyResults(), true);
            recalculateProgress(okr);
        }
        if (Boolean.TRUE.equals(req.getPublished()) && !Boolean.TRUE.equals(okr.getPublished())) {
            publishInternal(okr);
        }
        return toResponse(okr);
    }

    @Override
    @Transactional
    public void delete(String id) {
        Okr okr = find(id);
        scopeResolver.assertCanModify(okr.getOwnerPersonId());
        okr.softDelete(SystemUtils.getCurrentUsername());
        okrRepository.save(okr);
    }

    @Override
    @Transactional
    public OkrResponse checkIn(String id, OkrCheckInRequest req) {
        Okr okr = find(id);
        scopeResolver.assertCanModify(okr.getOwnerPersonId());
        if (req.getKeyResults() != null) {
            for (OkrKeyResultRequest krReq : req.getKeyResults()) {
                if (krReq.getId() == null) continue;
                keyResultRepository.findById(krReq.getId()).ifPresent(kr -> {
                    if (!id.equals(kr.getOkrId()) || Boolean.TRUE.equals(kr.getIsDeleted())) {
                        throw new AppException(CommonErrorCode.FORBIDDEN, "Key Result không thuộc OKR này");
                    }
                    if (krReq.getCurrentValue() != null) kr.setCurrentValue(krReq.getCurrentValue());
                    kr.setProgress(calcKrProgress(kr.getCurrentValue(), kr.getTargetValue()));
                    keyResultRepository.save(kr);
                });
            }
        }
        recalculateProgress(okr);
        return toResponse(okr);
    }

    @Override
    @Transactional
    public OkrResponse publish(String id) {
        Okr okr = find(id);
        publishInternal(okr);
        return toResponse(okr);
    }

    private void publishInternal(Okr okr) {
        scopeResolver.assertCanPublish(okr.getScopeType(), okr.getOwnerPersonId(), okr.getDepartmentId());
        okr.setPublished(true);
        okr.setPublishedAt(LocalDateTime.now());
        okr.setPublishedBy(SystemUtils.getCurrentUsername());
        if ("DRAFT".equalsIgnoreCase(okr.getStatus())) okr.setStatus("ACTIVE");
        okrRepository.save(okr);
    }

    private void saveKeyResults(String okrId, List<OkrKeyResultRequest> list, boolean createNew) {
        if (list == null) return;
        int order = 0;
        for (OkrKeyResultRequest r : list) {
            OkrKeyResult kr = OkrKeyResult.builder()
                    .okrId(okrId)
                    .title(r.getTitle())
                    .targetValue(r.getTargetValue())
                    .currentValue(r.getCurrentValue() != null ? r.getCurrentValue() : 0.0)
                    .unit(r.getUnit())
                    .sortOrder(r.getSortOrder() != null ? r.getSortOrder() : order++)
                    .build();
            kr.setProgress(calcKrProgress(kr.getCurrentValue(), kr.getTargetValue()));
            kr.setId(UUID.randomUUID().toString());
            keyResultRepository.save(kr);
        }
    }

    private void recalculateProgress(Okr okr) {
        List<OkrKeyResult> krs = keyResultRepository.findByOkrIdAndIsDeletedFalseOrderBySortOrderAsc(okr.getId());
        if (krs.isEmpty()) {
            okr.setProgress(0.0);
        } else {
            double avg = krs.stream()
                    .mapToDouble(kr -> kr.getProgress() != null ? kr.getProgress() : 0.0)
                    .average().orElse(0.0);
            okr.setProgress(Math.round(avg * 10.0) / 10.0);
        }
        okrRepository.save(okr);
    }

    private static Double calcKrProgress(Double current, Double target) {
        if (target == null || target == 0) return 0.0;
        double cur = current != null ? current : 0.0;
        return Math.min(100.0, Math.round((cur / target) * 1000.0) / 10.0);
    }

    private Okr find(String id) {
        return okrRepository.findById(id)
                .filter(o -> Boolean.FALSE.equals(o.getIsDeleted()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "OKR không tồn tại"));
    }

    private OkrResponse toResponse(Okr okr) {
        List<OkrKeyResultResponse> krs = keyResultRepository
                .findByOkrIdAndIsDeletedFalseOrderBySortOrderAsc(okr.getId())
                .stream()
                .map(kr -> OkrKeyResultResponse.builder()
                        .id(kr.getId())
                        .title(kr.getTitle())
                        .targetValue(kr.getTargetValue())
                        .currentValue(kr.getCurrentValue())
                        .unit(kr.getUnit())
                        .progress(kr.getProgress())
                        .sortOrder(kr.getSortOrder())
                        .build())
                .toList();
        return OkrResponse.builder()
                .id(okr.getId())
                .title(okr.getTitle())
                .description(okr.getDescription())
                .ownerPersonId(okr.getOwnerPersonId())
                .periodLabel(okr.getPeriodLabel())
                .cycleId(okr.getCycleId())
                .departmentId(okr.getDepartmentId())
                .orgId(okr.getOrgId())
                .parentOkrId(okr.getParentOkrId())
                .scopeType(okr.getScopeType())
                .objectiveType(okr.getObjectiveType())
                .crossLinkIds(split(okr.getCrossLinkIds()))
                .published(Boolean.TRUE.equals(okr.getPublished()))
                .startDate(okr.getStartDate())
                .endDate(okr.getEndDate())
                .status(okr.getStatus())
                .progress(okr.getProgress())
                .progressPct(okr.getProgress())
                .keyResults(krs)
                .build();
    }

    private void validateRequest(OkrRequest req, boolean creating) {
        if (creating && (req.getTitle() == null || req.getTitle().isBlank())) {
            throw new AppException(CommonErrorCode.VALIDATION_FAILED, "Mục tiêu là bắt buộc");
        }
        if (req.getStartDate() != null && req.getEndDate() != null
                && req.getStartDate().isAfter(req.getEndDate())) {
            throw new AppException(CommonErrorCode.VALIDATION_FAILED, "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }
        if (req.getParentOkrId() != null && req.getCrossLinkIds() != null
                && req.getCrossLinkIds().contains(req.getParentOkrId())) {
            throw new AppException(CommonErrorCode.VALIDATION_FAILED, "OKR cha không được trùng liên kết chéo");
        }
    }

    private String normalizeObjectiveType(String value) {
        String normalized = value == null ? "COMMITTED" : value.trim().toUpperCase();
        if (!List.of("COMMITTED", "STRETCH").contains(normalized)) {
            throw new AppException(CommonErrorCode.VALIDATION_FAILED, "Loại OKR không hợp lệ");
        }
        return normalized;
    }

    private String normalizeScopeType(String value) {
        String normalized = value == null ? "PERSONAL" : value.trim().toUpperCase();
        if (!List.of("PERSONAL", "TEAM", "DEPARTMENT", "COMPANY").contains(normalized)) {
            throw new AppException(CommonErrorCode.VALIDATION_FAILED, "Phạm vi OKR không hợp lệ");
        }
        return normalized;
    }

    private String join(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join(",", values);
    }

    private List<String> split(String value) {
        return value == null || value.isBlank() ? List.of() : Arrays.asList(value.split(","));
    }
}
