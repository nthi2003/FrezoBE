package com.frezo.qtbv.service.impl;

import com.frezo.auth.repository.UserRepository;
import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.helper.SystemUtils;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.repository.PersonRepository;
import com.frezo.qtbv.common.ArticleStatus;
import com.frezo.qtbv.dto.request.ArticlePinRequest;
import com.frezo.qtbv.dto.request.NewsCategoryRequest;
import com.frezo.qtbv.dto.request.NewsMottoRequest;
import com.frezo.qtbv.dto.response.*;
import com.frezo.qtbv.entity.*;
import com.frezo.qtbv.mapper.ArticleMapper;
import com.frezo.qtbv.repository.*;
import com.frezo.qtbv.service.ArticleService;
import com.frezo.qtbv.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private static final int MAX_PINS_PER_ORG = 5;

    private final NewsCategoryRepository categoryRepo;
    private final NewsMottoRepository mottoRepo;
    private final ArticlePinRepository pinRepo;
    private final ArticleRepository articleRepo;
    private final BannerRepository bannerRepo;
    private final ArticleMapper articleMapper;
    private final ArticleService articleService;
    private final UserRepository userRepository;
    private final PersonRepository personRepository;

    @Override
    public List<NewsCategoryResponse> listCategories(String organizationId) {
        List<NewsCategory> rows = StringUtils.hasText(organizationId)
                ? categoryRepo.findByOrganizationIdAndIsDeletedFalseOrderByOrderIndexAscNameAsc(organizationId)
                : categoryRepo.findByIsDeletedFalseOrderByOrderIndexAscNameAsc();
        if (StringUtils.hasText(organizationId)) {
            List<NewsCategory> global = categoryRepo.findByOrganizationIdIsNullAndIsDeletedFalseOrderByOrderIndexAscNameAsc();
            Map<String, NewsCategory> merged = new LinkedHashMap<>();
            for (NewsCategory c : global) merged.put(c.getId(), c);
            for (NewsCategory c : rows) merged.put(c.getId(), c);
            return merged.values().stream().map(this::toCategoryDto).collect(Collectors.toList());
        }
        return rows.stream().map(this::toCategoryDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NewsCategoryResponse createCategory(NewsCategoryRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new AppException(CommonErrorCode.VALIDATION_FAILED, "name");
        }
        NewsCategory saved = categoryRepo.save(NewsCategory.builder()
                .name(request.getName().trim())
                .color(StringUtils.hasText(request.getColor()) ? request.getColor().trim() : "#16a34a")
                .organizationId(blankToNull(request.getOrganizationId()))
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                .build());
        return toCategoryDto(saved);
    }

    @Override
    @Transactional
    public NewsCategoryResponse updateCategory(String id, NewsCategoryRequest request) {
        NewsCategory cat = getCategory(id);
        if (StringUtils.hasText(request.getName())) cat.setName(request.getName().trim());
        if (StringUtils.hasText(request.getColor())) cat.setColor(request.getColor().trim());
        if (request.getOrganizationId() != null) cat.setOrganizationId(blankToNull(request.getOrganizationId()));
        if (request.getOrderIndex() != null) cat.setOrderIndex(request.getOrderIndex());
        return toCategoryDto(categoryRepo.save(cat));
    }

    @Override
    @Transactional
    public void deleteCategory(String id) {
        NewsCategory cat = getCategory(id);
        cat.softDelete(SystemUtils.getCurrentUsername());
        categoryRepo.save(cat);
    }

    @Override
    public List<NewsMottoResponse> listMottos() {
        return mottoRepo.findByIsDeletedFalseOrderByCreatedDateDesc().stream()
                .map(this::toMottoDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NewsMottoResponse createMotto(NewsMottoRequest request) {
        if (!StringUtils.hasText(request.getContent())) {
            throw new AppException(CommonErrorCode.VALIDATION_FAILED, "content");
        }
        NewsMotto saved = mottoRepo.save(NewsMotto.builder()
                .content(request.getContent().trim())
                .author(StringUtils.hasText(request.getAuthor()) ? request.getAuthor().trim() : null)
                .build());
        return toMottoDto(saved);
    }

    @Override
    @Transactional
    public NewsMottoResponse updateMotto(String id, NewsMottoRequest request) {
        NewsMotto motto = getMotto(id);
        if (StringUtils.hasText(request.getContent())) motto.setContent(request.getContent().trim());
        if (request.getAuthor() != null) {
            motto.setAuthor(StringUtils.hasText(request.getAuthor()) ? request.getAuthor().trim() : null);
        }
        return toMottoDto(mottoRepo.save(motto));
    }

    @Override
    @Transactional
    public void deleteMotto(String id) {
        NewsMotto motto = getMotto(id);
        motto.softDelete(SystemUtils.getCurrentUsername());
        mottoRepo.save(motto);
    }

    @Override
    public List<ArticleResponse> listPins(String organizationId) {
        if (!StringUtils.hasText(organizationId)) return List.of();
        return resolvePinnedArticles(organizationId);
    }

    @Override
    @Transactional
    public ArticleResponse pinArticle(ArticlePinRequest request) {
        if (!StringUtils.hasText(request.getArticleId()) || !StringUtils.hasText(request.getOrganizationId())) {
            throw new AppException(CommonErrorCode.VALIDATION_FAILED, "articleId/organizationId");
        }
        Article article = articleRepo.findByIdNotDeleted(request.getArticleId());
        if (article == null || !ArticleStatus.PUBLISHED.equals(article.getStatus())) {
            throw new AppException("article.not.published", HttpStatus.BAD_REQUEST);
        }
        String orgId = request.getOrganizationId().trim();
        Optional<ArticlePin> existing = pinRepo.findByOrganizationIdAndArticleIdAndIsDeletedFalse(orgId, article.getId());
        if (existing.isPresent()) {
            return articleService.findById(article.getId());
        }
        long count = pinRepo.countByOrganizationIdAndIsDeletedFalse(orgId);
        if (count >= MAX_PINS_PER_ORG) {
            throw new AppException("article.pin.limit", HttpStatus.BAD_REQUEST, MAX_PINS_PER_ORG);
        }
        int sortOrder = request.getSortOrder() != null
                ? request.getSortOrder()
                : (int) count + 1;
        pinRepo.save(ArticlePin.builder()
                .articleId(article.getId())
                .organizationId(orgId)
                .sortOrder(sortOrder)
                .build());
        return articleService.findById(article.getId());
    }

    @Override
    @Transactional
    public void unpinArticle(String organizationId, String articleId) {
        pinRepo.findByOrganizationIdAndArticleIdAndIsDeletedFalse(organizationId, articleId)
                .ifPresent(pin -> {
                    pin.softDelete(SystemUtils.getCurrentUsername());
                    pinRepo.save(pin);
                });
    }

    @Override
    public NewsPageDataResponse getNewsPageData(String organizationId) {
        String resolvedOrgId = resolveOrganizationId(organizationId);

        List<BannerResponse> banners = bannerRepo
                .findByPinForNewsPageTrueAndStatusAndIsDeletedFalseOrderByOrderIndexAsc("ACTIVE")
                .stream()
                .filter(b -> !StringUtils.hasText(b.getOrganizationId())
                        || !StringUtils.hasText(resolvedOrgId)
                        || resolvedOrgId.equals(b.getOrganizationId()))
                .map(this::toBannerDto)
                .collect(Collectors.toList());

        List<NewsMotto> mottos = mottoRepo.findByIsDeletedFalseOrderByCreatedDateDesc();
        NewsMottoResponse motto = mottos.isEmpty() ? null : toMottoDto(mottos.get(0));

        List<NewsCategoryResponse> categories = listCategories(resolvedOrgId);
        List<ArticleResponse> articles = articleService.getHomeFeed();
        enrichCategoryMeta(articles);

        List<ArticleResponse> pinned = StringUtils.hasText(resolvedOrgId)
                ? resolvePinnedArticles(resolvedOrgId)
                : List.of();
        enrichCategoryMeta(pinned);

        Set<String> pinnedIds = pinned.stream().map(ArticleResponse::getId).collect(Collectors.toSet());
        List<ArticleResponse> feed = articles.stream()
                .filter(a -> !pinnedIds.contains(a.getId()))
                .collect(Collectors.toList());

        return NewsPageDataResponse.builder()
                .banners(banners)
                .motto(motto)
                .categories(categories)
                .pinnedArticles(pinned)
                .articles(feed)
                .resolvedOrganizationId(resolvedOrgId)
                .build();
    }

    private String resolveOrganizationId(String organizationId) {
        if (StringUtils.hasText(organizationId)) {
            return organizationId.trim();
        }
        return resolveCurrentUserOrganizationId();
    }

    private String resolveCurrentUserOrganizationId() {
        String username = SystemUtils.getCurrentUsername();
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return userRepository.findByUserName(username)
                .map(user -> user.getPersonId())
                .filter(StringUtils::hasText)
                .flatMap(personRepository::findById)
                .map(Person::getOrgId)
                .filter(StringUtils::hasText)
                .orElse(null);
    }

    private List<ArticleResponse> resolvePinnedArticles(String organizationId) {
        List<ArticlePin> pins = pinRepo.findByOrganizationIdAndIsDeletedFalseOrderBySortOrderAsc(organizationId);
        List<ArticleResponse> result = new ArrayList<>();
        for (ArticlePin pin : pins) {
            Article article = articleRepo.findByIdNotDeleted(pin.getArticleId());
            if (article == null) continue;
            if (!ArticleStatus.PUBLISHED.equals(article.getStatus()) || !Boolean.TRUE.equals(article.getIsActive())) {
                continue;
            }
            if (Boolean.FALSE.equals(article.getDisplayOnNews())) continue;
            ArticleResponse dto = articleMapper.toDto(article);
            result.add(dto);
        }
        enrichCategoryMeta(result);
        return result;
    }

    private void enrichCategoryMeta(List<ArticleResponse> articles) {
        if (articles == null || articles.isEmpty()) return;
        Set<String> catIds = articles.stream()
                .map(ArticleResponse::getCategoryId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (catIds.isEmpty()) return;
        Map<String, NewsCategory> byId = categoryRepo.findAllById(catIds).stream()
                .collect(Collectors.toMap(NewsCategory::getId, c -> c, (a, b) -> a));
        for (ArticleResponse a : articles) {
            if (!StringUtils.hasText(a.getCategoryId())) continue;
            NewsCategory cat = byId.get(a.getCategoryId());
            if (cat != null) {
                a.setCategoryName(cat.getName());
                a.setCategoryColor(cat.getColor());
            }
        }
    }

    private NewsCategory getCategory(String id) {
        return categoryRepo.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, id));
    }

    private NewsMotto getMotto(String id) {
        return mottoRepo.findById(id)
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, id));
    }

    private NewsCategoryResponse toCategoryDto(NewsCategory c) {
        return NewsCategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .color(c.getColor())
                .organizationId(c.getOrganizationId())
                .orderIndex(c.getOrderIndex())
                .build();
    }

    private NewsMottoResponse toMottoDto(NewsMotto m) {
        return NewsMottoResponse.builder()
                .id(m.getId())
                .content(m.getContent())
                .author(m.getAuthor())
                .build();
    }

    private BannerResponse toBannerDto(Banner b) {
        return BannerResponse.builder()
                .id(b.getId())
                .title(b.getTitle())
                .subtitle(b.getSubtitle())
                .imageUrl(b.getImageUrl())
                .linkUrl(b.getLinkUrl())
                .position(b.getPosition())
                .status(b.getStatus())
                .orderIndex(b.getOrderIndex())
                .organizationId(b.getOrganizationId())
                .pinForNewsPage(b.getPinForNewsPage())
                .build();
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
