package com.frezo.qtht.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.helper.SystemUtils;
import com.frezo.qtht.constant.QthtErrorCode;
import com.frezo.qtht.dto.request.GuideSaveRequest;
import com.frezo.qtht.dto.response.GuideResponse;
import com.frezo.qtht.dto.response.GuideSummaryResponse;
import com.frezo.qtht.entity.Guide;
import com.frezo.qtht.repository.GuideRepository;
import com.frezo.qtht.service.GuideService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class GuideServiceImpl implements GuideService {

    private final GuideRepository guideRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GuideSummaryResponse> listPublished() {
        return guideRepository.findByPublishedTrueAndIsDeletedFalseOrderBySortOrderAscTitleAsc()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GuideResponse getPublishedBySlug(String slug) {
        Guide guide = guideRepository.findBySlugAndPublishedTrueAndIsDeletedFalse(normalizeSlug(slug))
                .orElseThrow(() -> new AppException(QthtErrorCode.GUIDE_NOT_FOUND, slug));
        return toResponse(guide);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuideSummaryResponse> listAll() {
        return guideRepository.findByIsDeletedFalseOrderBySortOrderAscTitleAsc()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GuideResponse getById(String id) {
        return toResponse(requireGuide(id));
    }

    @Override
    @Transactional
    public GuideResponse create(GuideSaveRequest request) {
        String slug = normalizeSlug(request.getSlug());
        if (guideRepository.existsBySlugAndIsDeletedFalse(slug)) {
            throw new AppException(QthtErrorCode.GUIDE_SLUG_EXISTS, slug);
        }
        Guide guide = Guide.builder()
                .slug(slug)
                .title(request.getTitle().trim())
                .body(request.getBody())
                .module(trimToNull(request.getModule()))
                .summary(trimToNull(request.getSummary()))
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .published(Boolean.TRUE.equals(request.getPublished()))
                .build();
        return toResponse(guideRepository.save(guide));
    }

    @Override
    @Transactional
    public GuideResponse update(String id, GuideSaveRequest request) {
        Guide guide = requireGuide(id);
        String slug = normalizeSlug(request.getSlug());
        if (guideRepository.existsBySlugAndIsDeletedFalseAndIdNot(slug, id)) {
            throw new AppException(QthtErrorCode.GUIDE_SLUG_EXISTS, slug);
        }
        guide.setSlug(slug);
        guide.setTitle(request.getTitle().trim());
        guide.setBody(request.getBody());
        guide.setModule(trimToNull(request.getModule()));
        guide.setSummary(trimToNull(request.getSummary()));
        if (request.getSortOrder() != null) {
            guide.setSortOrder(request.getSortOrder());
        }
        if (request.getPublished() != null) {
            guide.setPublished(request.getPublished());
        }
        return toResponse(guideRepository.save(guide));
    }

    @Override
    @Transactional
    public GuideResponse publish(String id) {
        Guide guide = requireGuide(id);
        guide.setPublished(true);
        return toResponse(guideRepository.save(guide));
    }

    @Override
    @Transactional
    public GuideResponse unpublish(String id) {
        Guide guide = requireGuide(id);
        guide.setPublished(false);
        return toResponse(guideRepository.save(guide));
    }

    @Override
    @Transactional
    public void delete(String id) {
        Guide guide = requireGuide(id);
        guide.softDelete(SystemUtils.getCurrentUsername());
        guideRepository.save(guide);
    }

    private Guide requireGuide(String id) {
        return guideRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(QthtErrorCode.GUIDE_NOT_FOUND, id));
    }

    private GuideResponse toResponse(Guide g) {
        return GuideResponse.builder()
                .id(g.getId())
                .slug(g.getSlug())
                .title(g.getTitle())
                .body(g.getBody())
                .module(g.getModule())
                .summary(g.getSummary())
                .sortOrder(g.getSortOrder())
                .published(Boolean.TRUE.equals(g.getPublished()))
                .createdBy(g.getCreatedBy())
                .createdDate(g.getCreatedDate())
                .updatedBy(g.getUpdatedBy())
                .updatedDate(g.getUpdatedDate())
                .build();
    }

    private GuideSummaryResponse toSummary(Guide g) {
        return GuideSummaryResponse.builder()
                .id(g.getId())
                .slug(g.getSlug())
                .title(g.getTitle())
                .module(g.getModule())
                .summary(g.getSummary())
                .sortOrder(g.getSortOrder())
                .published(Boolean.TRUE.equals(g.getPublished()))
                .updatedBy(g.getUpdatedBy())
                .updatedDate(g.getUpdatedDate())
                .build();
    }

    private static String normalizeSlug(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
