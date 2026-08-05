package com.frezo.qtbv.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.helper.SystemUtils;
import com.frezo.qtbv.entity.Banner;
import com.frezo.qtbv.repository.BannerRepository;
import com.frezo.qtbv.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;

    @Override
    public List<Banner> findAll() {
        return bannerRepository.findByIsDeletedFalseOrderByPositionAscOrderIndexAsc();
    }

    @Override
    public Banner findById(String id) {
        return bannerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, id));
    }

    @Override
    @Transactional
    public Banner create(Banner request) {
        Banner banner = Banner.builder()
                .title(requireTitle(request.getTitle()))
                .subtitle(request.getSubtitle())
                .imageUrl(requireImageUrl(request.getImageUrl()))
                .linkUrl(request.getLinkUrl())
                .position(StringUtils.hasText(request.getPosition()) ? request.getPosition() : "hero")
                .status(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "ACTIVE")
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                .build();
        return bannerRepository.save(banner);
    }

    @Override
    @Transactional
    public Banner update(String id, Banner request) {
        Banner banner = findById(id);
        if (StringUtils.hasText(request.getTitle())) {
            banner.setTitle(request.getTitle().trim());
        }
        banner.setSubtitle(request.getSubtitle());
        if (StringUtils.hasText(request.getImageUrl())) {
            banner.setImageUrl(request.getImageUrl().trim());
        }
        banner.setLinkUrl(request.getLinkUrl());
        if (StringUtils.hasText(request.getPosition())) {
            banner.setPosition(request.getPosition());
        }
        if (StringUtils.hasText(request.getStatus())) {
            banner.setStatus(request.getStatus());
        }
        if (request.getOrderIndex() != null) {
            banner.setOrderIndex(request.getOrderIndex());
        }
        return bannerRepository.save(banner);
    }

    @Override
    @Transactional
    public void delete(String id) {
        Banner banner = findById(id);
        banner.softDelete(SystemUtils.getCurrentUsername());
        bannerRepository.save(banner);
    }

    private static String requireTitle(String title) {
        if (!StringUtils.hasText(title)) {
            throw new AppException(CommonErrorCode.VALIDATION_FAILED, "title");
        }
        return title.trim();
    }

    private static String requireImageUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            throw new AppException(CommonErrorCode.VALIDATION_FAILED, "imageUrl");
        }
        return imageUrl.trim();
    }
}
