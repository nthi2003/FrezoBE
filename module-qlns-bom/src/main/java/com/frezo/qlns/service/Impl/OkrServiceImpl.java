package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.helper.SystemUtils;
import com.frezo.qlns.dto.request.OkrCheckInRequest;
import com.frezo.qlns.dto.request.OkrKeyResultRequest;
import com.frezo.qlns.dto.request.OkrRequest;
import com.frezo.qlns.dto.response.OkrKeyResultResponse;
import com.frezo.qlns.dto.response.OkrResponse;
import com.frezo.qlns.entity.Okr;
import com.frezo.qlns.entity.OkrKeyResult;
import com.frezo.qlns.repository.OkrKeyResultRepository;
import com.frezo.qlns.repository.OkrRepository;
import com.frezo.qlns.service.OkrService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OkrServiceImpl implements OkrService {

    private final OkrRepository okrRepository;
    private final OkrKeyResultRepository keyResultRepository;

    @Override
    public List<OkrResponse> list(String ownerPersonId) {
        List<Okr> list = ownerPersonId != null
                ? okrRepository.findByOwnerPersonIdAndIsDeletedFalse(ownerPersonId)
                : okrRepository.findByIsDeletedFalseOrderByCreatedDateDesc();
        return list.stream().map(this::toResponse).toList();
    }

    @Override
    public OkrResponse get(String id) {
        return toResponse(find(id));
    }

    @Override
    @Transactional
    public OkrResponse create(OkrRequest req) {
        Okr okr = Okr.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .ownerPersonId(req.getOwnerPersonId())
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
        if (req.getTitle() != null) okr.setTitle(req.getTitle());
        if (req.getDescription() != null) okr.setDescription(req.getDescription());
        if (req.getOwnerPersonId() != null) okr.setOwnerPersonId(req.getOwnerPersonId());
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
        okr.softDelete(SystemUtils.getCurrentUsername());
        okrRepository.save(okr);
    }

    @Override
    @Transactional
    public OkrResponse checkIn(String id, OkrCheckInRequest req) {
        Okr okr = find(id);
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
