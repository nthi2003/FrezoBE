package com.frezo.qtbv.service.impl;

import com.frezo.qtbv.dto.request.ArticleReactionRequest;
import com.frezo.qtbv.dto.response.ArticleReactionResponse;
import com.frezo.qtbv.entity.ArticleReaction;
import com.frezo.qtbv.mapper.ArticleReactionMapper;
import com.frezo.qtbv.repository.ArticleReactionRepository;
import com.frezo.qtbv.service.ArticleReactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleReactionServiceImpl implements ArticleReactionService {

    private final ArticleReactionRepository articleReactionRepository;
    private final ArticleReactionMapper articleReactionMapper;
    @Override
    @Transactional
    public ArticleReactionResponse toggleReaction(String articleId, String userId,
                                                  ArticleReactionRequest request) {
        boolean exists = articleReactionRepository.existsByArticleIdAndUserIdAndType(
                articleId, userId, request.getType()
        );
        if(exists) {
            articleReactionRepository.deleteByArticleIdAndUserIdAndType(
                    articleId, userId, request.getType()
            );
            return  null;
        } else {
            ArticleReaction reaction = ArticleReaction.builder()
                    .articleId(articleId)
                    .userId(userId)
                    .type(request.getType())
                    .build();
            ArticleReaction saved = articleReactionRepository.save(reaction);
            return articleReactionMapper.toDto(saved);
        }
    }

    @Override
    @Transactional
    public Long countReactions(String articleId) {
        return articleReactionRepository.countByArticleId(articleId);
    }
}
