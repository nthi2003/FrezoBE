package com.frezo.fbautomation.service;

import com.frezo.fbautomation.dto.request.CommentRuleRequest;
import com.frezo.fbautomation.dto.request.ModeratedCommentRequest;
import com.frezo.fbautomation.dto.response.CommentRuleResponse;
import com.frezo.fbautomation.dto.response.ModeratedCommentResponse;

import java.util.List;
import java.util.Map;

public interface CommentModerationService {
    List<CommentRuleResponse> listRules();
    CommentRuleResponse getRule(String id);
    CommentRuleResponse createRule(CommentRuleRequest req);
    CommentRuleResponse updateRule(String id, CommentRuleRequest req);
    void deleteRule(String id);

    List<ModeratedCommentResponse> listComments(String status);
    ModeratedCommentResponse getComment(String id);
    ModeratedCommentResponse createComment(ModeratedCommentRequest req);
    ModeratedCommentResponse updateComment(String id, ModeratedCommentRequest req);
    void deleteComment(String id);
    ModeratedCommentResponse moderate(String id, String action, String replyText);

    Map<String, Object> dashboard();
}
