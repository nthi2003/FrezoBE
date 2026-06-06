package com.frezo.qtbv.service;

import com.frezo.qtbv.dto.request.ArticleReactionRequest;
import com.frezo.qtbv.dto.response.ArticleReactionResponse;

public interface ArticleReactionService {
    ArticleReactionResponse toggleReaction(String articleId, String userId, ArticleReactionRequest request);
    Long countReactions(String articleId);
}
