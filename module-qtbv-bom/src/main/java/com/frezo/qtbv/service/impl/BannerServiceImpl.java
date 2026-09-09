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

    /** Lấy toàn bộ danh sách banner chưa bị xoá, sắp xếp theo vị trí và thứ tự. */
    @Override
    public List<Banner> findAll() {
        return bannerRepository.findByIsDeletedFalseOrderByPositionAscOrderIndexAsc();
    }

    /** Lấy danh sách banner đang hoạt động (status ACTIVE) và chưa bị xoá, dùng cho hiển thị phía người dùng. */
    @Override
    @Transactional(readOnly = true)
    public List<Banner> findActive() {
        return bannerRepository.findByStatusAndIsDeletedFalseOrderByPositionAscOrderIndexAsc("ACTIVE");
    }

    /** Tìm banner theo id còn hoạt động, nếu không tồn tại thì ném lỗi NOT_FOUND. */
    @Override
    public Banner findById(String id) {
        return bannerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, id));
    }

    /** Tạo mới một banner với các trường bắt buộc (tiêu đề, hình ảnh), áp dụng giá trị mặc định cho các trường còn bỏ trống. */
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
                .organizationId(request.getOrganizationId())
                .pinForNewsPage(Boolean.TRUE.equals(request.getPinForNewsPage()))
                .build();
        return bannerRepository.save(banner);
    }

    /** Cập nhật các trường được thay đổi của banner theo id, chỉ ghi đè những trường có giá trị mới. */
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
        if (request.getOrganizationId() != null) {
            banner.setOrganizationId(StringUtils.hasText(request.getOrganizationId())
                    ? request.getOrganizationId().trim() : null);
        }
        if (request.getPinForNewsPage() != null) {
            banner.setPinForNewsPage(request.getPinForNewsPage());
        }
        return bannerRepository.save(banner);
    }

    /** Xoá mềm banner theo id: đánh dấu đã xoá và ghi thông tin người thao tác. */
    @Override
    @Transactional
    public void delete(String id) {
        Banner banner = findById(id);
        banner.softDelete(SystemUtils.getCurrentUsername());
        bannerRepository.save(banner);
    }

    /** Kiểm tra tiêu đề banner bắt buộc phải có, nếu rỗng thì ném lỗi validation, ngược lại trả về tiêu đề đã trim. */
    private static String requireTitle(String title) {
        if (!StringUtils.hasText(title)) {
            throw new AppException(CommonErrorCode.VALIDATION_FAILED, "title");
        }
        return title.trim();
    }

    /** Kiểm tra URL hình ảnh banner bắt buộc phải có, nếu rỗng thì ném lỗi validation, ngược lại trả về URL đã trim. */
    private static String requireImageUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            throw new AppException(CommonErrorCode.VALIDATION_FAILED, "imageUrl");
        }
        return imageUrl.trim();
    }
}
