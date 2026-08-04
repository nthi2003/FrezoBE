package com.frezo.fbautomation.service.impl;

import com.frezo.fbautomation.entity.AdCampaign;
import com.frezo.fbautomation.entity.AffiliateLink;
import com.frezo.fbautomation.entity.FacebookLead;
import com.frezo.fbautomation.entity.SocialPost;
import com.frezo.fbautomation.repository.AdCampaignRepository;
import com.frezo.fbautomation.repository.AffiliateLinkRepository;
import com.frezo.fbautomation.repository.FacebookAccountRepository;
import com.frezo.fbautomation.repository.FacebookGroupRepository;
import com.frezo.fbautomation.repository.FacebookLeadRepository;
import com.frezo.fbautomation.repository.PageReviewRepository;
import com.frezo.fbautomation.repository.SocialPostRepository;
import com.frezo.fbautomation.service.MktInsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Insights MVP — tổng hợp từ DB Frezo (leads, posts, affiliate, ads, reviews).
 * Không gọi Meta Graph API (read_insights) — sẽ bổ sung khi có Page Token.
 */
@Service
@RequiredArgsConstructor
public class MktInsightsServiceImpl implements MktInsightsService {

    private final FacebookLeadRepository leadRepository;
    private final SocialPostRepository postRepository;
    private final AffiliateLinkRepository affiliateRepository;
    private final AdCampaignRepository adCampaignRepository;
    private final FacebookAccountRepository accountRepository;
    private final FacebookGroupRepository groupRepository;
    private final PageReviewRepository reviewRepository;

    @Override
    public Map<String, Object> dashboard() {
        List<FacebookLead> leads = leadRepository.findAll().stream()
                .filter(l -> !Boolean.TRUE.equals(l.getIsDeleted())).toList();
        List<SocialPost> posts = postRepository.findAll().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted())).toList();
        List<AffiliateLink> links = affiliateRepository.findAll().stream()
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted())).toList();
        List<AdCampaign> ads = adCampaignRepository.findAll().stream()
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted())).toList();

        long affClicks = links.stream().mapToLong(a -> a.getClickCount() == null ? 0L : a.getClickCount()).sum();
        long affConv = links.stream().mapToLong(a -> a.getConversionCount() == null ? 0L : a.getConversionCount()).sum();
        BigDecimal adSpend = ads.stream()
                .map(a -> a.getSpend() == null ? BigDecimal.ZERO : a.getSpend())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long adImpressions = ads.stream().mapToLong(a -> a.getImpressions() == null ? 0L : a.getImpressions()).sum();
        long adClicks = ads.stream().mapToLong(a -> a.getClicks() == null ? 0L : a.getClicks()).sum();
        long adLeads = ads.stream().mapToLong(a -> a.getLeads() == null ? 0L : a.getLeads()).sum();

        Map<String, Object> m = new HashMap<>();
        m.put("source", "frezo_aggregate");
        m.put("note", "Số liệu nội bộ Frezo — chưa đồng bộ Meta Graph Insights.");

        m.put("totalLeads", leads.size());
        m.put("leadsBySource", leads.stream().collect(Collectors.groupingBy(
                l -> l.getSource() == null ? "UNKNOWN" : l.getSource(), Collectors.counting())));
        m.put("leadsByStatus", leads.stream().collect(Collectors.groupingBy(
                l -> l.getStatus() == null ? "UNKNOWN" : l.getStatus(), Collectors.counting())));

        m.put("totalPosts", posts.size());
        m.put("postsPublished", posts.stream().filter(p -> "PUBLISHED".equals(p.getStatus())).count());
        m.put("postsScheduled", posts.stream().filter(p -> "SCHEDULED".equals(p.getStatus())).count());
        m.put("postsByChannel", posts.stream().collect(Collectors.groupingBy(
                p -> p.getChannel() == null ? "UNKNOWN" : p.getChannel(), Collectors.counting())));

        m.put("affiliateLinks", links.size());
        m.put("affiliateClicks", affClicks);
        m.put("affiliateConversions", affConv);
        m.put("affiliateConversionRate", affClicks > 0
                ? BigDecimal.valueOf(affConv).divide(BigDecimal.valueOf(affClicks), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        m.put("adCampaigns", ads.size());
        m.put("adSpend", adSpend);
        m.put("adImpressions", adImpressions);
        m.put("adClicks", adClicks);
        m.put("adLeads", adLeads);
        m.put("adCtr", adImpressions > 0
                ? BigDecimal.valueOf(adClicks).divide(BigDecimal.valueOf(adImpressions), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        m.put("fbAccounts", accountRepository.count());
        m.put("fbGroups", groupRepository.count());

        var reviews = reviewRepository.findAll().stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted())).toList();
        m.put("totalReviews", reviews.size());
        m.put("averageRating", reviews.isEmpty() ? BigDecimal.ZERO :
                BigDecimal.valueOf(reviews.stream().mapToInt(r -> r.getRating() == null ? 0 : r.getRating()).average().orElse(0))
                        .setScale(2, RoundingMode.HALF_UP));
        m.put("lowRatingReviews", reviews.stream().filter(r -> r.getRating() != null && r.getRating() <= 2).count());

        return m;
    }
}
