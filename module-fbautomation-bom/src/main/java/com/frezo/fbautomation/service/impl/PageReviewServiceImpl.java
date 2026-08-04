package com.frezo.fbautomation.service.impl;

import com.frezo.fbautomation.dto.request.PageReviewRequest;
import com.frezo.fbautomation.dto.response.PageReviewResponse;
import com.frezo.fbautomation.entity.PageReview;
import com.frezo.fbautomation.repository.PageReviewRepository;
import com.frezo.fbautomation.service.PageReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PageReviewServiceImpl implements PageReviewService {

    private final PageReviewRepository repository;

    @Override
    public List<PageReviewResponse> list(String status, String platform) {
        return repository.findAll().stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .filter(r -> status == null || status.isBlank() || status.equalsIgnoreCase(r.getStatus()))
                .filter(r -> platform == null || platform.isBlank() || platform.equalsIgnoreCase(r.getPlatform()))
                .sorted(Comparator.comparing(PageReview::getCreatedDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PageReviewResponse get(String id) {
        return toResponse(mustFind(id));
    }

    @Override
    @Transactional
    public PageReviewResponse create(PageReviewRequest req) {
        PageReview r = PageReview.builder()
                .platform(nz(req.getPlatform(), "FACEBOOK"))
                .rating(req.getRating())
                .authorName(req.getAuthorName())
                .content(req.getContent())
                .status(nz(req.getStatus(), "NEW"))
                .replyText(req.getReplyText())
                .reviewedAt(req.getReviewedAt() != null ? req.getReviewedAt() : OffsetDateTime.now())
                .externalUrl(req.getExternalUrl())
                .note(req.getNote())
                .build();
        return toResponse(repository.save(r));
    }

    @Override
    @Transactional
    public PageReviewResponse update(String id, PageReviewRequest req) {
        PageReview r = mustFind(id);
        if (req.getPlatform() != null) r.setPlatform(req.getPlatform());
        r.setRating(req.getRating());
        r.setAuthorName(req.getAuthorName());
        r.setContent(req.getContent());
        if (req.getStatus() != null) r.setStatus(req.getStatus());
        r.setReplyText(req.getReplyText());
        if (req.getReviewedAt() != null) r.setReviewedAt(req.getReviewedAt());
        r.setExternalUrl(req.getExternalUrl());
        r.setNote(req.getNote());
        return toResponse(repository.save(r));
    }

    @Override
    @Transactional
    public void delete(String id) {
        PageReview r = mustFind(id);
        r.setIsDeleted(true);
        repository.save(r);
    }

    @Override
    @Transactional
    public PageReviewResponse reply(String id, String replyText) {
        PageReview r = mustFind(id);
        if (replyText == null || replyText.isBlank()) {
            throw new IllegalArgumentException("Nội dung trả lời bắt buộc");
        }
        r.setReplyText(replyText.trim());
        r.setStatus("REPLIED");
        return toResponse(repository.save(r));
    }

    @Override
    public Map<String, Object> dashboard() {
        List<PageReview> all = repository.findAll().stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted())).toList();
        double avg = all.isEmpty() ? 0d :
                all.stream().mapToInt(PageReview::getRating).average().orElse(0d);
        Map<String, Object> m = new HashMap<>();
        m.put("totalReviews", all.size());
        m.put("averageRating", BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        m.put("lowRatingCount", all.stream().filter(r -> r.getRating() != null && r.getRating() <= 2).count());
        m.put("newCount", all.stream().filter(r -> "NEW".equals(r.getStatus())).count());
        m.put("repliedCount", all.stream().filter(r -> "REPLIED".equals(r.getStatus())).count());
        m.put("byRating", all.stream().collect(Collectors.groupingBy(PageReview::getRating, Collectors.counting())));
        m.put("byPlatform", all.stream().collect(Collectors.groupingBy(
                r -> r.getPlatform() == null ? "OTHER" : r.getPlatform(), Collectors.counting())));
        return m;
    }

    private PageReview mustFind(String id) {
        PageReview r = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá"));
        if (Boolean.TRUE.equals(r.getIsDeleted())) throw new IllegalArgumentException("Đánh giá đã xoá");
        return r;
    }

    private PageReviewResponse toResponse(PageReview r) {
        PageReviewResponse out = new PageReviewResponse();
        out.setId(r.getId());
        out.setPlatform(r.getPlatform());
        out.setRating(r.getRating());
        out.setAuthorName(r.getAuthorName());
        out.setContent(r.getContent());
        out.setStatus(r.getStatus());
        out.setReplyText(r.getReplyText());
        out.setReviewedAt(r.getReviewedAt());
        out.setExternalUrl(r.getExternalUrl());
        out.setNote(r.getNote());
        out.setLowRating(r.getRating() != null && r.getRating() <= 2);
        out.setCreatedDate(r.getCreatedDate());
        return out;
    }

    private static String nz(String v, String def) {
        return v == null || v.isBlank() ? def : v.trim();
    }
}
