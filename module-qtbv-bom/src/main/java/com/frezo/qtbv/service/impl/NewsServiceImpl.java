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

    /** Liệt kê danh mục tin tức: riêng của tổ chức, hoặc gộp với các danh mục toàn cục khi có organizationId. */
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

    /** Tạo danh mục tin tức mới, yêu cầu tên bắt buộc và tự gán màu mặc định nếu bỏ trống. */
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

    /** Cập nhật thông tin danh mục tin tức theo id, chỉ ghi đè các trường có giá trị mới. */
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

    /** Xoá mềm một danh mục tin tức theo id. */
    @Override
    @Transactional
    public void deleteCategory(String id) {
        NewsCategory cat = getCategory(id);
        cat.softDelete(SystemUtils.getCurrentUsername());
        categoryRepo.save(cat);
    }

    /** Liệt kê các câu khẩu hiệu (motto) chưa bị xoá, xếp theo thời gian tạo giảm dần. */
    @Override
    public List<NewsMottoResponse> listMottos() {
        return mottoRepo.findByIsDeletedFalseOrderByCreatedDateDesc().stream()
                .map(this::toMottoDto)
                .collect(Collectors.toList());
    }

    /** Tạo mới một câu khẩu hiệu, yêu cầu nội dung bắt buộc. */
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

    /** Cập nhật nội dung hoặc tác giả của câu khẩu hiệu theo id. */
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

    /** Xoá mềm một câu khẩu hiệu theo id. */
    @Override
    @Transactional
    public void deleteMotto(String id) {
        NewsMotto motto = getMotto(id);
        motto.softDelete(SystemUtils.getCurrentUsername());
        mottoRepo.save(motto);
    }

    /** Liệt kê các bài viết được ghim của một tổ chức theo thứ tự sort order. */
    @Override
    public List<ArticleResponse> listPins(String organizationId) {
        if (!StringUtils.hasText(organizationId)) return List.of();
        return resolvePinnedArticles(organizationId);
    }

    /** Ghim một bài viết đã xuất bản vào tổ chức, giới hạn tối đa MAX_PINS_PER_ORG bài và tránh ghim trùng. */
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

    /** Bỏ ghim (xoá mềm) một bài viết khỏi tổ chức theo organizationId và articleId. */
    @Override
    @Transactional
    public void unpinArticle(String organizationId, String articleId) {
        pinRepo.findByOrganizationIdAndArticleIdAndIsDeletedFalse(organizationId, articleId)
                .ifPresent(pin -> {
                    pin.softDelete(SystemUtils.getCurrentUsername());
                    pinRepo.save(pin);
                });
    }

    /** Tổng hợp toàn bộ dữ liệu cho trang tin tức: banner, motto, danh mục, bài viết feed và bài ghim. */
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

    /** Xác định organizationId hiệu lực: dùng giá trị truyền vào hoặc suy ra từ tổ chức của người dùng hiện tại. */
    private String resolveOrganizationId(String organizationId) {
        if (StringUtils.hasText(organizationId)) {
            return organizationId.trim();
        }
        return resolveCurrentUserOrganizationId();
    }

    /** Suy ra mã tổ chức của người dùng đang đăng nhập từ user -> person -> orgId. */
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

    /** Lấy các bài viết đã ghim còn hợp lệ (đã xuất bản, hoạt động, hiển thị trên tin tức) của một tổ chức. */
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

    /** Gán tên và màu chuyên mục cho danh sách bài viết dựa trên categoryId. */
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

    /** Tìm danh mục tin tức theo id còn hoạt động, nếu không tồn tại thì ném lỗi NOT_FOUND. */
    private NewsCategory getCategory(String id) {
        return categoryRepo.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, id));
    }

    /** Tìm câu khẩu hiệu theo id còn hoạt động, nếu không tồn tại thì ném lỗi NOT_FOUND. */
    private NewsMotto getMotto(String id) {
        return mottoRepo.findById(id)
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, id));
    }

    /** Chuyển đổi entity NewsCategory thành NewsCategoryResponse. */
    private NewsCategoryResponse toCategoryDto(NewsCategory c) {
        return NewsCategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .color(c.getColor())
                .organizationId(c.getOrganizationId())
                .orderIndex(c.getOrderIndex())
                .build();
    }

    /** Chuyển đổi entity NewsMotto thành NewsMottoResponse. */
    private NewsMottoResponse toMottoDto(NewsMotto m) {
        return NewsMottoResponse.builder()
                .id(m.getId())
                .content(m.getContent())
                .author(m.getAuthor())
                .build();
    }

    /** Chuyển đổi entity Banner thành BannerResponse. */
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

    /** Chuyển một chuỗi rỗng/trắng thành null, ngược lại trả về chuỗi đã trim. */
    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
