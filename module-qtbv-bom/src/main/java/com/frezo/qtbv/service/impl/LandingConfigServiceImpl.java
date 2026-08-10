package com.frezo.qtbv.service.impl;

import com.frezo.qtbv.entity.LandingConfig;
import com.frezo.qtbv.repository.LandingConfigRepository;
import com.frezo.qtbv.service.LandingConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LandingConfigServiceImpl implements LandingConfigService {
    private final LandingConfigRepository landingConfigRepository;

    @Override
    public LandingConfig getConfig() {
        return landingConfigRepository.findByIsActiveTrue()
                .orElse(new LandingConfig());
    }

    @Override
    @Transactional
    public LandingConfig updateConfig(LandingConfig config) {
        LandingConfig current = getConfig();
        current.setBrandName(config.getBrandName());
        current.setLogoUrl(config.getLogoUrl());
        current.setPrimaryColor(config.getPrimaryColor());
        current.setContactEmail(config.getContactEmail());
        current.setContactPhone(config.getContactPhone());
        current.setContactTitle(config.getContactTitle());
        current.setContactSubtitle(config.getContactSubtitle());
        current.setAboutUs(config.getAboutUs());
        current.setAboutTitle(config.getAboutTitle());
        current.setAboutSubtitle(config.getAboutSubtitle());
        current.setAboutImageUrl(config.getAboutImageUrl());
        current.setStoryTitle(config.getStoryTitle());
        current.setStorySubtitle(config.getStorySubtitle());
        current.setFooterText(config.getFooterText());
        current.setHeroTitle(config.getHeroTitle());
        current.setHeroSubtitle(config.getHeroSubtitle());
        current.setBlogTitle(config.getBlogTitle());
        current.setBlogSubtitle(config.getBlogSubtitle());
        current.setProductTitle(config.getProductTitle());
        current.setProductSubtitle(config.getProductSubtitle());
        current.setOpsTitle(config.getOpsTitle());
        current.setOpsSubtitle(config.getOpsSubtitle());
        current.setShippingPolicy(config.getShippingPolicy());
        current.setContactAddress(config.getContactAddress());
        current.setWorkingHours(config.getWorkingHours());
        current.setNewsletterTitle(config.getNewsletterTitle());
        current.setNewsletterSubtitle(config.getNewsletterSubtitle());

        // ---- SEO / Meta ----
        current.setSeoTitle(config.getSeoTitle());
        current.setSeoDescription(config.getSeoDescription());
        current.setSeoKeywords(config.getSeoKeywords());
        current.setOgImageUrl(config.getOgImageUrl());
        current.setFaviconUrl(config.getFaviconUrl());
        current.setHeroImageUrl(config.getHeroImageUrl());
        current.setHeroMediaType(config.getHeroMediaType() != null ? config.getHeroMediaType() : "SLIDER");
        current.setHeroVideoUrl(config.getHeroVideoUrl());
        current.setCampaignName(config.getCampaignName());
        current.setCampaignStartDate(config.getCampaignStartDate());
        current.setCampaignEndDate(config.getCampaignEndDate());
        current.setLoadingTitle(config.getLoadingTitle());
        current.setLoadingSubtitle(config.getLoadingSubtitle());
        current.setLoadingImageUrl(config.getLoadingImageUrl());
        current.setCanonicalUrl(config.getCanonicalUrl());

        // ---- Social links ----
        current.setFacebookUrl(config.getFacebookUrl());
        current.setInstagramUrl(config.getInstagramUrl());
        current.setYoutubeUrl(config.getYoutubeUrl());
        current.setTiktokUrl(config.getTiktokUrl());
        current.setZaloUrl(config.getZaloUrl());

        // ---- Analytics ----
        current.setGtmId(config.getGtmId());
        current.setGa4Id(config.getGa4Id());
        current.setFbPixelId(config.getFbPixelId());
        current.setIsActive(true);

        return landingConfigRepository.save(current);
    }
}
