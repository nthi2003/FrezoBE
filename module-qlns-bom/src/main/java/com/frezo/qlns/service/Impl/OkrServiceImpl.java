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
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .status(req.getStatus() != null ? req.getStatus() : "ACTIVE")
                .progress(0.0)
                .build();
        okr.setId(UUID.randomUUID().toString());
        okr = okrRepository.save(okr);
        saveKeyResults(okr.getId(), req.getKeyResults(), true);
        recalculateProgress(okr);
        return toResponse(okr);
    }

    @Override
    @Transactional
    public OkrResponse update(String id, OkrRequest req) {
        Okr okr = find(id);
        scopeResolver.assertCanModify(okr.getOwnerPersonId());
        if (req.getTitle() != null) okr.setTitle(req.getTitle());
        if (req.getDescription() != null) okr.setDescription(req.getDescription());
        if (req.getOwnerPersonId() != null) {
            scopeResolver.assertCanAssignOwner(req.getOwnerPersonId());
            okr.setOwnerPersonId(req.getOwnerPersonId());
        }
        if (req.getPeriodLabel() != null) okr.setPeriodLabel(req.getPeriodLabel());
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
                    if (krReq.getCurrentValue() != null) kr.setCurrentValue(krReq.getCurrentValue());
                    kr.setProgress(calcKrProgress(kr.getCurrentValue(), kr.getTargetValue()));
                    keyResultRepository.save(kr);
                });
            }
        }
        recalculateProgress(okr);
        return toResponse(okr);
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
                .startDate(okr.getStartDate())
                .endDate(okr.getEndDate())
                .status(okr.getStatus())
                .progress(okr.getProgress())
                .progressPct(okr.getProgress())
                .keyResults(krs)
                .build();
    }
}
