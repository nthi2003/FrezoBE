package com.frezo.qtbv.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.qtbv.common.ArticleStatus;
import com.frezo.qtbv.common.PublishScope;
import com.frezo.qtbv.config.SecurityHelper;
import com.frezo.qtbv.dto.request.ArticleCreateRequest;
import com.frezo.qtbv.dto.request.ArticleFilterRequest;
import com.frezo.qtbv.dto.request.ArticleReviewRequest;
import com.frezo.qtbv.dto.request.ArticleUpdateRequest;
import com.frezo.qtbv.dto.response.ArticleResponse;
import com.frezo.qtbv.entity.Article;
import com.frezo.qtbv.entity.ArticleRevision;
import com.frezo.qtbv.mapper.ArticleMapper;
import com.frezo.qtbv.repository.ArticleRepository;
import com.frezo.qtbv.repository.ArticleRevisionRepository;
import com.frezo.qtbv.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleServiceImpl implements ArticleService {

    private static final String ERR_ARTICLE_CODE_EXISTS = "article.code.exists";
    private static final String ERR_PERMISSION_EDIT = "article.permission.edit";
    private static final String ERR_PERMISSION_DELETE = "article.permission.delete";
    private static final String ERR_PERMISSION_PUBLISH = "article.permission.publish";
    private static final String ERR_PERMISSION_REVIEW = "article.permission.review";
    private static final String ERR_STATUS_INVALID_EDIT = "article.status.invalid.edit";
    private static final String ERR_STATUS_INVALID_SUBMIT = "article.status.invalid.submit";
    private static final String ERR_STATUS_INVALID_PUBLISH = "article.status.invalid.publish";
    private static final String ERR_STATUS_INVALID_REVIEW = "article.status.invalid.review";
    private static final String ERR_ARTICLE_NOT_FOUND = "article.not.found";

    private final ArticleRepository articleRepository;
    private final ArticleMapper articleMapper;
    private final SecurityHelper helper;
    private final ArticleRevisionRepository articleRevisionRepository;

    @Override
    @Transactional
    public ArticleResponse create(ArticleCreateRequest request) {
        if (Boolean.TRUE.equals(articleRepository.existsByCode(request.getCode()))) {
            throw new AppException(ERR_ARTICLE_CODE_EXISTS, HttpStatus.BAD_REQUEST, request.getCode());
        }

        Article article = articleMapper.toEntityFromCreateRequest(request);
        String currentUserId = helper.getCurrentUserId();

        article.setAuthorId(currentUserId);
        article.setManagerId(request.getManagerId());
        article.setStatus(ArticleStatus.DRAFT);
        article.setIsActive(false);
        article.setIsDeleted(false);

        if (request.getOrganizationId() != null) {
            article.setOrganizationId(request.getOrganizationId());
        }

        Article saved = articleRepository.save(article);

        ArticleResponse response = articleMapper.toDto(saved);
        response.setHeartCount(0L);
        return response;
    }

    @Override
    @Transactional
    public ArticleResponse update(String id, ArticleUpdateRequest request, String authorId) {
        Article article = getArticleByIdOrThrow(id);

        validateAuthorPermission(article, authorId, ERR_PERMISSION_EDIT);

        if (article.getStatus() != ArticleStatus.DRAFT && article.getStatus() != ArticleStatus.REJECTED) {
            throw new AppException(ERR_STATUS_INVALID_EDIT, HttpStatus.BAD_REQUEST);
        }

        articleMapper.updateEntityFromRequest(request, article);
        articleRepository.save(article);

        ArticleResponse response = articleMapper.toDto(article);
        enrichArticlesWithHeartCount(Collections.singletonList(response));
        return response;
    }

    @Override
    @Transactional
    public void submitForApproval(String id, String authorId) {
        Article article = getArticleByIdOrThrow(id);

        validateAuthorPermission(article, authorId, ERR_PERMISSION_EDIT);

        if (article.getStatus() != ArticleStatus.DRAFT && article.getStatus() != ArticleStatus.REJECTED) {
            throw new AppException(ERR_STATUS_INVALID_SUBMIT, HttpStatus.BAD_REQUEST);
        }

        article.setStatus(ArticleStatus.WAITING_APPROVAL);
        articleRepository.save(article);
    }

    @Override
    @Transactional
    public void delete(String id, String authorId) {
        Article article = getArticleByIdOrThrow(id);

        validateAuthorPermission(article, authorId, ERR_PERMISSION_DELETE);

        article.setStatus(ArticleStatus.DELETED);
        article.setIsDeleted(true);
        article.setDeletedAt(LocalDateTime.now());
        articleRepository.save(article);
    }

    @Override
    @Transactional
    public ArticleResponse publish(String id, String managerId) {
        Article article = getArticleByIdOrThrow(id);

        if (!ArticleStatus.APPROVED.equals(article.getStatus())) {
            throw new AppException(ERR_STATUS_INVALID_PUBLISH, HttpStatus.BAD_REQUEST);
        }
        if (!article.getManagerId().equals(managerId)) {
            throw new AppException(ERR_PERMISSION_PUBLISH, HttpStatus.FORBIDDEN);
        }

        List<ArticleRevision> revisions = articleRevisionRepository.findLastByArticleId(id);
        if (!revisions.isEmpty()) {
            ArticleRevision lastRevision = revisions.get(0);
            article.setContent(lastRevision.getNewContent());
            articleRevisionRepository.deleteAll(revisions);
        }

        article.setStatus(ArticleStatus.PUBLISHED);
        article.setIsActive(true);
        article.setPublishedAt(LocalDateTime.now());

        articleRepository.save(article);

        ArticleResponse response = articleMapper.toDto(article);
        enrichArticlesWithHeartCount(Collections.singletonList(response));
        return response;
    }

    @Override
    public List<ArticleResponse> getPublishedArticles(String organizationId) {
        List<ArticleResponse> responses = articleRepository.findPublishedArticles().stream()
                .filter(a -> a.getPublishScope() == PublishScope.PUBLIC ||
                        (organizationId != null && organizationId.equals(a.getOrganizationId())))
                .map(articleMapper::toDto)
                .collect(Collectors.toList());

        enrichArticlesWithHeartCount(responses);
        return responses;
    }

    @Override
    @Transactional
    public ArticleResponse review(String id, ArticleReviewRequest request, String managerId) {
        Article article = getArticleByIdOrThrow(id);

        if (!ArticleStatus.WAITING_APPROVAL.equals(article.getStatus())) {
            throw new AppException(ERR_STATUS_INVALID_REVIEW, HttpStatus.BAD_REQUEST);
        }

        if (!article.getManagerId().equals(managerId)) {
            throw new AppException(ERR_PERMISSION_REVIEW, HttpStatus.FORBIDDEN);
        }

        article.setStatus(Boolean.TRUE.equals(request.getApproved()) ? ArticleStatus.APPROVED : ArticleStatus.REJECTED);
        articleRepository.save(article);

        ArticleResponse response = articleMapper.toDto(article);
        enrichArticlesWithHeartCount(Collections.singletonList(response));
        return response;
    }

    @Override
    public ArticleResponse findById(String id) {
        Article article = getArticleByIdOrThrow(id);
        ArticleResponse response = articleMapper.toDto(article);
        enrichArticlesWithHeartCount(Collections.singletonList(response));
        return response;
    }

    @Override
    public Map<String, Object> filter(ArticleFilterRequest request) {
        final ArticleFilterRequest filter = (request == null) ? new ArticleFilterRequest() : request;
        Page<Article> page = articleRepository.findAll(createSpecification(filter), filter.toPageable());

        List<ArticleResponse> responses = page.getContent().stream()
                .map(articleMapper::toDto)
                .collect(Collectors.toList());

        // Batch fetch heart counts
        enrichArticlesWithHeartCount(responses);

        // Filter by minHeart (In-memory filtering - Note: may affect page size)
        if (filter.getMinHeart() != null) {
            final Integer minHeart = filter.getMinHeart();
            responses = responses.stream()
                    .filter(a -> a.getHeartCount() >= minHeart)
                    .toList();
        }

        return Map.of(
                "items", responses,
                "total", page.getTotalElements(),
                "pageNumber", page.getNumber(),
                "pageSize", page.getSize());
    }

    @Override
    public Map<String, Object> getPublicArticles(Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(
                pageNumber == null ? 0 : pageNumber,
                pageSize == null ? 10 : pageSize);

        Page<Article> entities = articleRepository.findPublicArticles(pageable);
        List<ArticleResponse> responses = entities.getContent().stream()
                .map(articleMapper::toDto)
                .collect(Collectors.toList());

        enrichArticlesWithHeartCount(responses);

        return ServiceHelper.createResponse1(pageNumber, pageSize, entities, responses);
    }

    @Override
    public Map<String, Object> getMyDrafts(Integer pageNumber, Integer pageSize, String authorId) {
        Pageable pageable = PageRequest.of(
                pageNumber == null ? 0 : pageNumber,
                pageSize == null ? 10 : pageSize);

        Page<Article> page = articleRepository.findByAuthorIdAndIsDeletedFalse(authorId, pageable);
        List<ArticleResponse> responses = page.getContent().stream()
                .map(articleMapper::toDto)
                .collect(Collectors.toList());

        enrichArticlesWithHeartCount(responses);

        return ServiceHelper.createResponse1(pageNumber, pageSize, page, responses);
    }

    @Override
    public Map<String, Object> getPendingApproval(Integer pageNumber, Integer pageSize, String managerId) {
        Pageable pageable = PageRequest.of(
                pageNumber == null ? 0 : pageNumber,
                pageSize == null ? 10 : pageSize);

        Page<Article> page = articleRepository.findWaitingApprovalByManagerId(managerId, pageable);
        List<ArticleResponse> responses = page.getContent().stream()
                .map(articleMapper::toDto)
                .collect(Collectors.toList());

        enrichArticlesWithHeartCount(responses);

        return ServiceHelper.createResponse1(pageNumber, pageSize, page, responses);
    }

    // --- Helper Methods ---

    private Article getArticleByIdOrThrow(String id) {
        Article article = articleRepository.findByIdNotDeleted(id);
        if (article == null) {
            throw new AppException(ERR_ARTICLE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        return article;
    }

    private void validateAuthorPermission(Article article, String authorId, String errorMsg) {
        if (!article.getAuthorId().equals(authorId)) {
            throw new AppException(errorMsg, HttpStatus.FORBIDDEN);
        }
    }

    private void enrichArticlesWithHeartCount(List<ArticleResponse> responses) {
        if (responses == null || responses.isEmpty())
            return;

        List<String> articleIds = responses.stream()
                .map(ArticleResponse::getId)
                .toList();

        List<Object[]> counts = articleRepository.countReactionsByArticleIds(articleIds);
        Map<String, Long> countMap = counts.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1],
                        (existing, replacement) -> existing));

        for (ArticleResponse response : responses) {
            response.setHeartCount(countMap.getOrDefault(response.getId(), 0L));
        }
    }

    private Specification<Article> createSpecification(ArticleFilterRequest f) {
        Specification<Article> spec = Specification.where(GenericSpecification.hasFieldIs("isDeleted", false));

        if (SystemUtils.isNotNullOrEmpty(f.getTitle()))
            spec = spec.and(GenericSpecification.likeField("title", f.getTitle()));

        if (SystemUtils.isNotNullOrEmpty(f.getCode()))
            spec = spec.and(GenericSpecification.likeField("code", f.getCode()));

        if (SystemUtils.isNotNullOrEmpty(f.getAuthorId()))
            spec = spec.and(GenericSpecification.equalField("authorId", f.getAuthorId()));

        if (SystemUtils.isNotNullOrEmpty(f.getManagerId()))
            spec = spec.and(GenericSpecification.equalField("managerId", f.getManagerId()));

        if (f.getStatus() != null)
            spec = spec.and(GenericSpecification.equalField("status", f.getStatus()));

        if (f.getPublishScope() != null)
            spec = spec.and(GenericSpecification.equalField("publishScope", f.getPublishScope()));

        if (SystemUtils.isNotNullOrEmpty(f.getOrganizationId()))
            spec = spec.and(GenericSpecification.equalField("organizationId", f.getOrganizationId()));

        if (f.getPublishDateFrom() != null)
            spec = spec.and(GenericSpecification.greaterThanOrEqual("publishedAt", f.getPublishDateFrom()));

        if (f.getPublishDateTo() != null)
            spec = spec.and(GenericSpecification.lessThanOrEqual("publishedAt", f.getPublishDateTo()));

        return spec;
    }
}
