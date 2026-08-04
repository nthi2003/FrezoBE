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
    @Override
    public List<CommentRuleResponse> listRules() {
        return ruleRepository.findAll().stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .sorted(Comparator.comparing(CommentModerationRule::getCreatedDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toRuleResponse)
                .toList();
    }

    @Override
    public CommentRuleResponse getRule(String id) {
        return toRuleResponse(mustFindRule(id));
    }

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

    @Override
    @Transactional
    public void deleteRule(String id) {
        CommentModerationRule r = mustFindRule(id);
        r.setIsDeleted(true);
        ruleRepository.save(r);
    }

    // ---- Comments ----
    @Override
    public List<ModeratedCommentResponse> listComments(String status) {
        return commentRepository.findAll().stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .filter(c -> status == null || status.isBlank() || status.equalsIgnoreCase(c.getStatus()))
                .sorted(Comparator.comparing(ModeratedComment::getCreatedDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toCommentResponse)
                .toList();
    }

    @Override
    public ModeratedCommentResponse getComment(String id) {
        return toCommentResponse(mustFindComment(id));
    }

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

    @Override
    @Transactional
    public void deleteComment(String id) {
        ModeratedComment c = mustFindComment(id);
        c.setIsDeleted(true);
        commentRepository.save(c);
    }

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

    private CommentModerationRule mustFindRule(String id) {
        CommentModerationRule r = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy rule"));
        if (Boolean.TRUE.equals(r.getIsDeleted())) throw new IllegalArgumentException("Rule đã xoá");
        return r;
    }

    private ModeratedComment mustFindComment(String id) {
        ModeratedComment c = commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy comment"));
        if (Boolean.TRUE.equals(c.getIsDeleted())) throw new IllegalArgumentException("Comment đã xoá");
        return c;
    }

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

    private static String nz(String v, String def) {
        return v == null || v.isBlank() ? def : v.trim();
    }
}
