package com.frezo.fbautomation.service.impl;

import com.frezo.fbautomation.dto.request.CommentRuleRequest;
import com.frezo.fbautomation.dto.request.ModeratedCommentRequest;
import com.frezo.fbautomation.dto.response.CommentRuleResponse;
import com.frezo.fbautomation.dto.response.ModeratedCommentResponse;
import com.frezo.fbautomation.entity.CommentModerationRule;
import com.frezo.fbautomation.entity.ModeratedComment;
import com.frezo.fbautomation.repository.CommentModerationRuleRepository;
import com.frezo.fbautomation.repository.ModeratedCommentRepository;
import com.frezo.fbautomation.service.CommentModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentModerationServiceImpl implements CommentModerationService {

    private final CommentModerationRuleRepository ruleRepository;
    private final ModeratedCommentRepository commentRepository;

    // ---- Rules ----
    /** Lấy danh sách tất cả rule kiểm duyệt chưa bị xóa, sắp xếp mới nhất trước. */
    @Override
    public List<CommentRuleResponse> listRules() {
        return ruleRepository.findAll().stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .sorted(Comparator.comparing(CommentModerationRule::getCreatedDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toRuleResponse)
                .toList();
    }

    /** Lấy thông tin chi tiết một rule kiểm duyệt theo mã định danh. */
    @Override
    public CommentRuleResponse getRule(String id) {
        return toRuleResponse(mustFindRule(id));
    }

    /** Tạo mới rule kiểm duyệt bình luận từ thông tin trong CommentRuleRequest. */
    @Override
    @Transactional
    public CommentRuleResponse createRule(CommentRuleRequest req) {
        CommentModerationRule r = CommentModerationRule.builder()
                .name(req.getName().trim())
                .keywords(req.getKeywords().trim())
                .action(nz(req.getAction(), "FLAG"))
                .replyTemplate(req.getReplyTemplate())
                .enabled(req.getEnabled() == null || req.getEnabled())
                .note(req.getNote())
                .hitCount(0L)
                .build();
        return toRuleResponse(ruleRepository.save(r));
    }

    /** Cập nhật thông tin rule kiểm duyệt theo mã định danh. */
    @Override
    @Transactional
    public CommentRuleResponse updateRule(String id, CommentRuleRequest req) {
        CommentModerationRule r = mustFindRule(id);
        r.setName(req.getName().trim());
        r.setKeywords(req.getKeywords().trim());
        if (req.getAction() != null) r.setAction(req.getAction());
        r.setReplyTemplate(req.getReplyTemplate());
        if (req.getEnabled() != null) r.setEnabled(req.getEnabled());
        r.setNote(req.getNote());
        return toRuleResponse(ruleRepository.save(r));
    }

    /** Xóa mềm rule kiểm duyệt theo mã định danh. */
    @Override
    @Transactional
    public void deleteRule(String id) {
        CommentModerationRule r = mustFindRule(id);
        r.setIsDeleted(true);
        ruleRepository.save(r);
    }

    // ---- Comments ----
    /** Lấy danh sách bình luận cần kiểm duyệt, có thể lọc theo trạng thái. */
    @Override
    public List<ModeratedCommentResponse> listComments(String status) {
        return commentRepository.findAll().stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .filter(c -> status == null || status.isBlank() || status.equalsIgnoreCase(c.getStatus()))
                .sorted(Comparator.comparing(ModeratedComment::getCreatedDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toCommentResponse)
                .toList();
    }

    /** Lấy thông tin chi tiết bình luận theo mã định danh. */
    @Override
    public ModeratedCommentResponse getComment(String id) {
        return toCommentResponse(mustFindComment(id));
    }

    /** Tạo mới bình luận và tự động đối chiếu các rule để gắn kết quả kiểm duyệt ban đầu. */
    @Override
    @Transactional
    public ModeratedCommentResponse createComment(ModeratedCommentRequest req) {
        ModeratedComment c = ModeratedComment.builder()
                .platform(nz(req.getPlatform(), "FACEBOOK"))
                .authorName(req.getAuthorName())
                .content(req.getContent().trim())
                .postUrl(req.getPostUrl())
                .status(nz(req.getStatus(), "PENDING"))
                .replyText(req.getReplyText())
                .commentedAt(req.getCommentedAt() != null ? req.getCommentedAt() : OffsetDateTime.now())
                .note(req.getNote())
                .build();
        matchRules(c);
        return toCommentResponse(commentRepository.save(c));
    }

    /** Cập nhật thông tin bình luận theo mã định danh. */
    @Override
    @Transactional
    public ModeratedCommentResponse updateComment(String id, ModeratedCommentRequest req) {
        ModeratedComment c = mustFindComment(id);
        if (req.getPlatform() != null) c.setPlatform(req.getPlatform());
        c.setAuthorName(req.getAuthorName());
        c.setContent(req.getContent().trim());
        c.setPostUrl(req.getPostUrl());
        if (req.getStatus() != null) c.setStatus(req.getStatus());
        c.setReplyText(req.getReplyText());
        if (req.getCommentedAt() != null) c.setCommentedAt(req.getCommentedAt());
        c.setNote(req.getNote());
        return toCommentResponse(commentRepository.save(c));
    }

    /** Xóa mềm bình luận theo mã định danh. */
    @Override
    @Transactional
    public void deleteComment(String id) {
        ModeratedComment c = mustFindComment(id);
        c.setIsDeleted(true);
        commentRepository.save(c);
    }

    /** Xử lý thủ công một bình luận theo hành động HIDE, IGNORE, FLAG hoặc REPLY. */
    @Override
    @Transactional
    public ModeratedCommentResponse moderate(String id, String action, String replyText) {
        ModeratedComment c = mustFindComment(id);
        String act = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
        switch (act) {
            case "HIDE" -> c.setStatus("HIDDEN");
            case "IGNORE" -> c.setStatus("IGNORED");
            case "FLAG" -> c.setStatus("FLAGGED");
            case "REPLY" -> {
                c.setStatus("REPLIED");
                if (replyText != null && !replyText.isBlank()) c.setReplyText(replyText.trim());
            }
            default -> throw new IllegalArgumentException("action phải là HIDE|REPLY|FLAG|IGNORE");
        }
        return toCommentResponse(commentRepository.save(c));
    }

    /** Tổng hợp số liệu bảng điều khiển kiểm duyệt: tổng bình luận, rule và phân loại theo trạng thái. */
    @Override
    public Map<String, Object> dashboard() {
        List<ModeratedComment> comments = commentRepository.findAll().stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted())).toList();
        List<CommentModerationRule> rules = ruleRepository.findAll().stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted())).toList();
        Map<String, Object> m = new HashMap<>();
        m.put("totalComments", comments.size());
        m.put("pending", comments.stream().filter(c -> "PENDING".equals(c.getStatus())).count());
        m.put("hidden", comments.stream().filter(c -> "HIDDEN".equals(c.getStatus())).count());
        m.put("replied", comments.stream().filter(c -> "REPLIED".equals(c.getStatus())).count());
        m.put("flagged", comments.stream().filter(c -> "FLAGGED".equals(c.getStatus())).count());
        m.put("totalRules", rules.size());
        m.put("enabledRules", rules.stream().filter(r -> Boolean.TRUE.equals(r.getEnabled())).count());
        m.put("byStatus", comments.stream().collect(Collectors.groupingBy(ModeratedComment::getStatus, Collectors.counting())));
        return m;
    }

    /** Đối chiếu nội dung bình luận với các rule đang bật; nếu khớp keyword thì gắn rule, cập nhật trạng thái và tăng bộ đếm hit. */
    private void matchRules(ModeratedComment c) {
        String content = c.getContent() == null ? "" : c.getContent().toLowerCase(Locale.ROOT);
        for (CommentModerationRule rule : ruleRepository.findByEnabledTrue()) {
            if (Boolean.TRUE.equals(rule.getIsDeleted())) continue;
            boolean hit = Arrays.stream(rule.getKeywords().split(","))
                    .map(String::trim)
                    .filter(k -> !k.isEmpty())
                    .anyMatch(k -> content.contains(k.toLowerCase(Locale.ROOT)));
            if (hit) {
                c.setMatchedRuleId(rule.getId());
                c.setMatchedRuleName(rule.getName());
                if ("HIDE".equalsIgnoreCase(rule.getAction())) c.setStatus("FLAGGED");
                else if ("FLAG".equalsIgnoreCase(rule.getAction())) c.setStatus("FLAGGED");
                else if ("REPLY".equalsIgnoreCase(rule.getAction()) && rule.getReplyTemplate() != null) {
                    c.setReplyText(rule.getReplyTemplate());
                    c.setStatus("FLAGGED");
                }
                rule.setHitCount((rule.getHitCount() == null ? 0L : rule.getHitCount()) + 1);
                ruleRepository.save(rule);
                break;
            }
        }
    }

    /** Tìm rule kiểm duyệt theo mã, ném ngoại lệ nếu không tồn tại hoặc đã xóa mềm. */
    private CommentModerationRule mustFindRule(String id) {
        CommentModerationRule r = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy rule"));
        if (Boolean.TRUE.equals(r.getIsDeleted())) throw new IllegalArgumentException("Rule đã xoá");
        return r;
    }

    /** Tìm bình luận theo mã, ném ngoại lệ nếu không tồn tại hoặc đã xóa mềm. */
    private ModeratedComment mustFindComment(String id) {
        ModeratedComment c = commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy comment"));
        if (Boolean.TRUE.equals(c.getIsDeleted())) throw new IllegalArgumentException("Comment đã xoá");
        return c;
    }

    /** Chuyển đổi thực thể CommentModerationRule sang DTO Response. */
    private CommentRuleResponse toRuleResponse(CommentModerationRule r) {
        CommentRuleResponse out = new CommentRuleResponse();
        out.setId(r.getId());
        out.setName(r.getName());
        out.setKeywords(r.getKeywords());
        out.setAction(r.getAction());
        out.setReplyTemplate(r.getReplyTemplate());
        out.setEnabled(r.getEnabled());
        out.setHitCount(r.getHitCount() == null ? 0L : r.getHitCount());
        out.setNote(r.getNote());
        out.setCreatedDate(r.getCreatedDate());
        return out;
    }

    /** Chuyển đổi thực thể ModeratedComment sang DTO Response. */
    private ModeratedCommentResponse toCommentResponse(ModeratedComment c) {
        ModeratedCommentResponse out = new ModeratedCommentResponse();
        out.setId(c.getId());
        out.setPlatform(c.getPlatform());
        out.setAuthorName(c.getAuthorName());
        out.setContent(c.getContent());
        out.setPostUrl(c.getPostUrl());
        out.setStatus(c.getStatus());
        out.setMatchedRuleId(c.getMatchedRuleId());
        out.setMatchedRuleName(c.getMatchedRuleName());
        out.setReplyText(c.getReplyText());
        out.setCommentedAt(c.getCommentedAt());
        out.setNote(c.getNote());
        out.setCreatedDate(c.getCreatedDate());
        return out;
    }

    /** Trả về giá trị mặc định nếu chuỗi null hoặc rỗng. */
    private static String nz(String v, String def) {
        return v == null || v.isBlank() ? def : v.trim();
    }
}
