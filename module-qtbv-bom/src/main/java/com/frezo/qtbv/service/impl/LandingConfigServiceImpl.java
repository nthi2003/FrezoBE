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
        current.setAboutUs(config.getAboutUs());
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
        return landingConfigRepository.save(current);
    }
}
