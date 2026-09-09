package com.frezo.fbautomation.service.impl;

import com.frezo.fbautomation.dto.request.AffiliateLinkRequest;
import com.frezo.fbautomation.dto.response.AffiliateLinkResponse;
import com.frezo.fbautomation.entity.AffiliateClick;
import com.frezo.fbautomation.entity.AffiliateLink;
import com.frezo.fbautomation.repository.AffiliateClickRepository;
import com.frezo.fbautomation.repository.AffiliateLinkRepository;
import com.frezo.fbautomation.service.AffiliateLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * AffiliateLinkServiceImpl — CRUD + click tracker + conversion attribution.
 * <p>
 * Attribution model: last-click, 30-day window. Khi có conversion cho code X, tìm click
 * gần nhất của cùng IP trong 30 ngày → đánh dấu converted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AffiliateLinkServiceImpl implements AffiliateLinkService {

    private static final String ALPHABET =
            "abcdefghijkmnpqrstuvwxyz23456789"; // bỏ ký tự dễ nhầm: 0/O/o/l/1/I
    private static final int    SLUG_LEN = 8;
    private static final int    ATTR_DAYS = 30;

    private final AffiliateLinkRepository linkRepository;
    private final AffiliateClickRepository clickRepository;

    @Value("${frezo.affiliate.base-url:http://localhost:3000}")
    private String baseUrl;

    private final SecureRandom rng = new SecureRandom();

    // ============================================================
    //  CRUD
    // ============================================================
    /**
     * Tạo mới link affiliate với mã code duy nhất.
     * Nếu không cung cấp code thì tự sinh slug ngẫu nhiên 8 ký tự.
     *
     * @param req yêu cầu chứa thông tin link affiliate
     * @return link affiliate vừa được tạo dưới dạng DTO
     */
    @Override
    @Transactional
    public AffiliateLinkResponse create(AffiliateLinkRequest req) {
        String code = (req.getCode() != null && !req.getCode().isBlank())
                ? req.getCode().trim()
                : generateUniqueSlug();
        if (linkRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Mã link '" + code + "' đã tồn tại — chọn mã khác");
        }
        AffiliateLink link = AffiliateLink.builder()
                .code(code)
                .targetUrl(req.getTargetUrl())
                .campaign(req.getCampaign())
                .kolName(req.getKolName())
                .kolContact(req.getKolContact())
                .utmSource(nz(req.getUtmSource(), "affiliate"))
                .utmMedium(nz(req.getUtmMedium(), "kol"))
                .utmCampaign(nz(req.getUtmCampaign(), req.getCampaign()))
                .utmTerm(req.getUtmTerm())
                .utmContent(req.getUtmContent())
                .commissionRate(req.getCommissionRate())
                .expiresAt(req.getExpiresAt())
                .note(req.getNote())
                .status("ACTIVE")
                .build();
        link = linkRepository.save(link);
        return toResponse(link);
    }

    /** Cập nhật thông tin link affiliate theo mã định danh. */
    @Override
    @Transactional
    public AffiliateLinkResponse update(String id, AffiliateLinkRequest req) {
        AffiliateLink link = mustFind(id);
        link.setTargetUrl(req.getTargetUrl());
        link.setCampaign(req.getCampaign());
        link.setKolName(req.getKolName());
        link.setKolContact(req.getKolContact());
        link.setUtmSource(req.getUtmSource());
        link.setUtmMedium(req.getUtmMedium());
        link.setUtmCampaign(req.getUtmCampaign());
        link.setUtmTerm(req.getUtmTerm());
        link.setUtmContent(req.getUtmContent());
        link.setCommissionRate(req.getCommissionRate());
        link.setExpiresAt(req.getExpiresAt());
        link.setNote(req.getNote());
        return toResponse(linkRepository.save(link));
    }

    /** Lấy thông tin link affiliate theo mã định danh. */
    @Override
    public AffiliateLinkResponse get(String id) { return toResponse(mustFind(id)); }

    /** Xóa mềm link affiliate theo mã định danh. */
    @Override
    @Transactional
    public void delete(String id) {
        AffiliateLink link = mustFind(id);
        link.setIsDeleted(true);
        linkRepository.save(link);
    }

    /** Lấy danh sách link affiliate, có thể lọc theo campaign, trạng thái và tên KOL. */
    @Override
    public List<AffiliateLinkResponse> list(String campaign, String status, String kolName) {
        var stream = linkRepository.findAll().stream()
                .filter(l -> !Boolean.TRUE.equals(l.getIsDeleted()));
        if (campaign != null && !campaign.isBlank())
            stream = stream.filter(l -> campaign.equalsIgnoreCase(l.getCampaign()));
        if (status != null && !status.isBlank())
            stream = stream.filter(l -> status.equalsIgnoreCase(l.getStatus()));
        if (kolName != null && !kolName.isBlank()) {
            String needle = kolName.toLowerCase();
            stream = stream.filter(l -> l.getKolName() != null && l.getKolName().toLowerCase().contains(needle));
        }
        return stream.sorted(Comparator.comparing(AffiliateLink::getCreatedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse).toList();
    }

    // ============================================================
    //  TRACKING
    // ============================================================
    /**
     * Theo dõi lượt nhấp chuột và trả về URL đích với tham số UTM.
     * Kiểm tra link còn ACTIVE và chưa hết hạn, ghi nhận click, cập nhật bộ đếm unique.
     *
     * @param code mã code link affiliate
     * @param ip địa chỉ IP người nhấp
     * @param userAgent chuỗi user-agent của trình duyệt
     * @param referer URL giới thiệu
     * @return URL đích đã gắn tham số UTM
     */
    @Override
    @Transactional
    public String trackAndResolve(String code, String ip, String userAgent, String referer) {
        AffiliateLink link = linkRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Link không tồn tại"));

        if (!"ACTIVE".equals(link.getStatus())) {
            throw new IllegalStateException("Link đã " + link.getStatus());
        }
        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(OffsetDateTime.now())) {
            link.setStatus("EXPIRED");
            linkRepository.save(link);
            throw new IllegalStateException("Link đã hết hạn");
        }

        // Log click.
        AffiliateClick click = AffiliateClick.builder()
                .linkId(link.getId())
                .code(link.getCode())
                .clickedAt(OffsetDateTime.now())
                .ip(ip)
                .userAgent(userAgent)
                .referer(referer)
                .converted(false)
                .build();
        clickRepository.save(click);

        // Counter update. Unique = chưa click bằng IP này trong 24h.
        linkRepository.incrementClick(link.getId());
        if (ip != null) {
            long recent = clickRepository.countByLinkIdAndIpAndClickedAtAfter(
                    link.getId(), ip, OffsetDateTime.now().minusHours(24));
            if (recent <= 1) linkRepository.incrementUniqueClick(link.getId());
        }

        return buildFinalUrl(link);
    }

    /**
     * Ghi nhận chuyển đổi (conversion) cho link affiliate theo mã code.
     * Áp dụng mô hình last-click attribution trong cửa sổ 30 ngày.
     *
     * @param code mã link affiliate
     * @param value giá trị chuyển đổi
     * @return true nếu ghi nhận thành công
     */
    @Override
    @Transactional
    public boolean recordConversion(String code, BigDecimal value) {
        AffiliateLink link = linkRepository.findByCode(code).orElse(null);
        if (link == null) return false;
        BigDecimal v = value != null ? value : BigDecimal.ZERO;
        linkRepository.incrementConversion(link.getId(), v);

        // Attribution: tìm click gần nhất chưa converted trong 30 ngày.
        var clicks = clickRepository.findByLinkId(link.getId()).stream()
                .filter(c -> !Boolean.TRUE.equals(c.getConverted()))
                .filter(c -> c.getClickedAt() != null && c.getClickedAt()
                        .isAfter(OffsetDateTime.now().minusDays(ATTR_DAYS)))
                .max(Comparator.comparing(AffiliateClick::getClickedAt))
                .orElse(null);
        if (clicks != null) {
            clicks.setConverted(true);
            clicks.setConversionValue(v);
            clicks.setConvertedAt(OffsetDateTime.now());
            clickRepository.save(clicks);
        }
        return true;
    }

    /**
     * Tổng hợp dữ liệu bảng điều khiển affiliate: tổng link, lượt nhấp, chuyển đổi,
     * doanh thu, hoa hồng ước tính và top KOL/campaign.
     *
     * @return map chứa tổng quan và xếp hạng
     */
    @Override
    public Map<String, Object> dashboard() {
        var links = linkRepository.findAll().stream()
                .filter(l -> !Boolean.TRUE.equals(l.getIsDeleted()))
                .toList();

        long totalClicks = links.stream().mapToLong(l -> Objects.requireNonNullElse(l.getClickCount(), 0L)).sum();
        long totalConv   = links.stream().mapToLong(l -> Objects.requireNonNullElse(l.getConversionCount(), 0L)).sum();
        BigDecimal revenue = links.stream()
                .map(l -> Objects.requireNonNullElse(l.getRevenue(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commissionEst = links.stream().map(l -> {
            var rev  = Objects.requireNonNullElse(l.getRevenue(), BigDecimal.ZERO);
            var rate = Objects.requireNonNullElse(l.getCommissionRate(), BigDecimal.ZERO);
            return rev.multiply(rate);
        }).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Top 5 KOL theo doanh thu.
        var topKol = links.stream()
                .filter(l -> l.getKolName() != null)
                .collect(Collectors.groupingBy(AffiliateLink::getKolName,
                        Collectors.reducing(BigDecimal.ZERO,
                                l -> Objects.requireNonNullElse(l.getRevenue(), BigDecimal.ZERO),
                                BigDecimal::add)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(e -> Map.of("kol", e.getKey(), "revenue", e.getValue()))
                .toList();

        // Top 5 campaign.
        var topCampaign = links.stream()
                .filter(l -> l.getCampaign() != null)
                .collect(Collectors.groupingBy(AffiliateLink::getCampaign,
                        Collectors.summingLong(l -> Objects.requireNonNullElse(l.getClickCount(), 0L))))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> Map.of("campaign", e.getKey(), "clicks", e.getValue()))
                .toList();

        Map<String, Object> dash = new HashMap<>();
        dash.put("totalLinks",       links.size());
        dash.put("totalClicks",      totalClicks);
        dash.put("totalConversions", totalConv);
        dash.put("totalRevenue",     revenue);
        dash.put("estimatedCommission", commissionEst);
        dash.put("conversionRate",   totalClicks == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(totalConv).divide(BigDecimal.valueOf(totalClicks), 4, RoundingMode.HALF_UP));
        dash.put("topKol", topKol);
        dash.put("topCampaign", topCampaign);
        return dash;
    }

    // ============================================================
    //  HELPERS
    // ============================================================
    /** Tìm link affiliate theo mã định danh, ném ngoại lệ nếu không tồn tại. */
    private AffiliateLink mustFind(String id) {
        return linkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Link không tồn tại: " + id));
    }

    /** Sinh slug ngẫu nhiên 8 ký tự từ bảng chữ cái an toàn, thử tối đa 5 lần để tìm mã duy nhất. */
    private String generateUniqueSlug() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder sb = new StringBuilder(SLUG_LEN);
            for (int i = 0; i < SLUG_LEN; i++) sb.append(ALPHABET.charAt(rng.nextInt(ALPHABET.length())));
            String candidate = sb.toString();
            if (!linkRepository.existsByCode(candidate)) return candidate;
        }
        throw new IllegalStateException("Không sinh được slug duy nhất sau 5 lần thử");
    }

    /** Trả về giá trị mặc định nếu chuỗi null hoặc rỗng. */
    private String nz(String v, String fallback) {
        return v != null && !v.isBlank() ? v : fallback;
    }

    /** Ghép tham số UTM và mã affiliate vào URL đích để tạo link theo dõi hoàn chỉnh. */
    private String buildFinalUrl(AffiliateLink link) {
        StringBuilder url = new StringBuilder(link.getTargetUrl());
        url.append(link.getTargetUrl().contains("?") ? "&" : "?");
        appendUtm(url, "utm_source",   link.getUtmSource());
        appendUtm(url, "utm_medium",   link.getUtmMedium());
        appendUtm(url, "utm_campaign", link.getUtmCampaign());
        appendUtm(url, "utm_term",     link.getUtmTerm());
        appendUtm(url, "utm_content",  link.getUtmContent());
        appendUtm(url, "aff",          link.getCode());
        if (url.charAt(url.length() - 1) == '&' || url.charAt(url.length() - 1) == '?') {
            url.deleteCharAt(url.length() - 1);
        }
        return url.toString();
    }

    /** Nối thêm tham số UTM (đã mã hóa UTF-8) vào URL nếu giá trị không rỗng. */
    private void appendUtm(StringBuilder url, String key, String value) {
        if (value == null || value.isBlank()) return;
        url.append(key).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8)).append('&');
    }

    /** Chuyển đổi thực thể AffiliateLink sang DTO Response kèm chỉ số hiệu suất (click, chuyển đổi, hoa hồng). */
    private AffiliateLinkResponse toResponse(AffiliateLink l) {
        AffiliateLinkResponse r = new AffiliateLinkResponse();
        r.setId(l.getId());
        r.setCode(l.getCode());
        r.setShortUrl(baseUrl + "/r/" + l.getCode());
        r.setTargetUrl(l.getTargetUrl());
        r.setTargetUrlWithUtm(buildFinalUrl(l));
        r.setCampaign(l.getCampaign());
        r.setKolName(l.getKolName());
        r.setKolContact(l.getKolContact());
        r.setUtmSource(l.getUtmSource());
        r.setUtmMedium(l.getUtmMedium());
        r.setUtmCampaign(l.getUtmCampaign());
        r.setUtmTerm(l.getUtmTerm());
        r.setUtmContent(l.getUtmContent());
        r.setCommissionRate(l.getCommissionRate());
        r.setStatus(l.getStatus());
        r.setExpiresAt(l.getExpiresAt());
        r.setClickCount(Objects.requireNonNullElse(l.getClickCount(), 0L));
        r.setUniqueClickCount(Objects.requireNonNullElse(l.getUniqueClickCount(), 0L));
        r.setConversionCount(Objects.requireNonNullElse(l.getConversionCount(), 0L));
        r.setRevenue(Objects.requireNonNullElse(l.getRevenue(), BigDecimal.ZERO));
        r.setCommissionPaid(Objects.requireNonNullElse(l.getCommissionPaid(), BigDecimal.ZERO));

        BigDecimal rev  = r.getRevenue();
        BigDecimal rate = Objects.requireNonNullElse(l.getCommissionRate(), BigDecimal.ZERO);
        r.setEstimatedCommission(rev.multiply(rate).subtract(r.getCommissionPaid()).max(BigDecimal.ZERO));

        r.setConversionRate(r.getClickCount() == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(r.getConversionCount())
                    .divide(BigDecimal.valueOf(r.getClickCount()), 4, RoundingMode.HALF_UP));

        r.setNote(l.getNote());
        r.setCreatedDate(l.getCreatedDate());
        return r;
    }
}
