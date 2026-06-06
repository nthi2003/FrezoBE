package com.frezo.qtbv.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.qtbv.common.ArticleStatus;
import com.frezo.qtbv.config.SecurityHelper;
import com.frezo.qtbv.dto.request.ArticleManagerEditRequest;
import com.frezo.qtbv.entity.Article;
import com.frezo.qtbv.entity.ArticleRevision;
import com.frezo.qtbv.repository.ArticleRepository;
import com.frezo.qtbv.repository.ArticleRevisionRepository;
import com.frezo.qtbv.service.ArticleVisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleVisionServiceImpl implements ArticleVisionService {
    private final ArticleRepository articleRepository;
    private final SecurityHelper helper;
    private final ArticleRevisionRepository articleRevisionRepository;

    @Override
    public void managerEdit(String articleId, ArticleManagerEditRequest request) {
        String managerId = helper.getCurrentUserId();
        Article article = articleRepository.findByIdNotDeleted(articleId);

       if (article.getStatus() != ArticleStatus.WAITING_APPROVAL) {
            throw new AppException("article.manager.edit.invalid.status",
                    HttpStatus.BAD_REQUEST);
        }

        if (!article.getManagerId().equals(managerId)) {
            throw new AppException("article.manager.edit.forbidden",
                    HttpStatus.FORBIDDEN);
        }

        String oldContent = article.getContent();

        ArticleRevision revision = ArticleRevision.builder()
                .articleId(articleId)
                .oldContent(oldContent)
                .newContent(request.getNewContent())
                .build();

        articleRevisionRepository.save(revision);
    }
}
