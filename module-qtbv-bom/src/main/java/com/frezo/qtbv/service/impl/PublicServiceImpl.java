package com.frezo.qtbv.service.impl;

import com.frezo.common.response.PageResponse;
import com.frezo.qtbv.dto.response.ArticleResponse;
import com.frezo.qtbv.entity.Banner;
import com.frezo.qtbv.service.BannerService;
import com.frezo.qtbv.entity.LandingConfig;
import com.frezo.qtbv.service.ArticleService;
import com.frezo.qtbv.service.LandingConfigService;
import com.frezo.qtbv.service.PublicService;
import com.frezo.product.dto.request.ProductFilterRequest;
import com.frezo.product.dto.response.ProductResponse;
import com.frezo.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicServiceImpl implements PublicService {

    private final LandingConfigService landingConfigService;
    private final BannerService bannerService;
    private final ProductService productService;
    private final ArticleService articleService;

    /** Lấy cấu hình landing page hiện tại cho khu vực công khai. */
    @Override
    public LandingConfig getLandingConfig() {
        return landingConfigService.getConfig();
    }

    /** Lấy danh sách banner đang hoạt động cho trang công khai. */
    @Override
    public List<Banner> getActiveBanners() {
        return bannerService.findActive();
    }

    /** Lấy danh sách sản phẩm công khai theo bộ lọc, luôn chỉ trả về sản phẩm đang hoạt động. */
    @Override
    public PageResponse<ProductResponse> getProducts(ProductFilterRequest filterRequest) {
        if (filterRequest == null) filterRequest = new ProductFilterRequest();
        filterRequest.setIsActive(true);
        return productService.filter(filterRequest);
    }

    /** Lấy chi tiết một sản phẩm công khai theo id. */
    @Override
    public ProductResponse getProductDetail(String id) {
        return productService.getById(id);
    }

    /** Lấy danh sách bài viết công khai theo phân trang. */
    @Override
    public PageResponse<ArticleResponse> getArticles(int page, int size) {
        return articleService.getPublicArticles(page, size);
    }

    /** Lấy chi tiết một bài viết công khai theo id. */
    @Override
    public Object getArticleDetail(String id) {
        return articleService.getPublicArticleById(id);
    }

    /** Sinh file sitemap XML chứa các URL chính và URL sản phẩm, bài viết đang hoạt động để phục vụ SEO của trang công khai. */
    @Override
    public String getSitemapXml() {
        LandingConfig config = landingConfigService.getConfig();
        String baseUrl = config != null && StringUtils.hasText(config.getCanonicalUrl())
                ? config.getCanonicalUrl().replaceAll("/+$", "")
                : "https://frezo.vn";

        ProductFilterRequest productFilter = new ProductFilterRequest();
        productFilter.setPage(0);
        productFilter.setSize(1000);
        productFilter.setIsActive(true);
        PageResponse<ProductResponse> products = productService.filter(productFilter);
        PageResponse<ArticleResponse> articles = articleService.getPublicArticles(0, 1000);

        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
        appendUrl(xml, baseUrl + "/", null, "daily", "1.0");
        appendUrl(xml, baseUrl + "/about", null, "monthly", "0.7");
        appendUrl(xml, baseUrl + "/products", null, "daily", "0.9");
        appendUrl(xml, baseUrl + "/blogs", null, "daily", "0.8");

        if (products != null && products.getItems() != null) {
            products.getItems().stream()
                    .filter(item -> item != null && StringUtils.hasText(item.getId()))
                    .forEach(item -> appendUrl(xml, baseUrl + "/products/" + item.getId(),
                            item.getCreatedDate(), "weekly", "0.8"));
        }
        if (articles != null && articles.getItems() != null) {
            articles.getItems().stream()
                    .filter(item -> item != null && StringUtils.hasText(item.getId()))
                    .forEach(item -> appendUrl(xml, baseUrl + "/blogs/" + item.getId(),
                            item.getUpdatedAt() != null ? item.getUpdatedAt() : item.getPublishedAt(),
                            "monthly", "0.7"));
        }
        return xml.append("</urlset>").toString();
    }

    /** Thêm một URL (location) vào chuỗi XML sitemap kèm thời gian sửa đổi, tần suất thay đổi và độ ưu tiên. */
    private static void appendUrl(StringBuilder xml, String location, LocalDateTime lastModified,
                                  String changeFrequency, String priority) {
        xml.append("<url><loc>").append(escapeXml(location)).append("</loc>");
        if (lastModified != null) {
            xml.append("<lastmod>")
                    .append(lastModified.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .append("</lastmod>");
        }
        xml.append("<changefreq>").append(changeFrequency).append("</changefreq>")
                .append("<priority>").append(priority).append("</priority></url>");
    }

    /** Thoát các ký tự đặc biệt XML (&, <, >, ", ') để tránh hỏng cấu trúc sitemap. */
    private static String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
