package com.frezo.qtht.service.impl;

import com.frezo.qtht.entity.IpWhitelist;
import com.frezo.qtht.repository.IpWhitelistRepository;
import com.frezo.qtht.service.IpWhitelistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IpWhitelistServiceImpl implements IpWhitelistService {

    private final IpWhitelistRepository ipWhitelistRepository;

    /**
     * Lấy danh sách tất cả các IP đang trong whitelist (isActive = true).
     */
    @Override
    public List<IpWhitelist> getAllActive() {
        return ipWhitelistRepository.findByIsActiveTrue();
    }

    /**
     * Lấy danh sách địa chỉ IP đang được phép (whitelist), chỉ trả về chuỗi IP.
     */
    @Override
    public List<String> getAllActiveWhitelistIps() {
        return getAllActive().stream()
                .map(IpWhitelist::getIpAddress)
                .collect(Collectors.toList());
    }

    /**
     * Thêm một IP vào danh sách trắng, kiểm tra trùng lặp trước khi lưu.
     */
    @Override
    @Transactional
    public IpWhitelist addToWhitelist(String ipAddress, String description, String createdBy) {
        if (ipWhitelistRepository.existsByIpAddress(ipAddress)) {
            throw new RuntimeException("IP đã tồn tại trong danh sách whitelist");
        }

        IpWhitelist whitelist = IpWhitelist.builder()
                .ipAddress(ipAddress)
                .description(description)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();

        return ipWhitelistRepository.save(whitelist);
    }

    /**
     * Gỡ IP khỏi danh sách trắng bằng cách đặt isActive = false (xoá mềm logic).
     */
    @Override
    @Transactional
    public void removeFromWhitelist(String id) {
        IpWhitelist whitelist = ipWhitelistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("IP không tồn tại trong whitelist"));
        whitelist.setIsActive(false);
        ipWhitelistRepository.save(whitelist);
    }

    /**
     * Kiểm tra xem một địa chỉ IP có nằm trong danh sách trắng (whitelist) và đang hoạt động hay không.
     */
    @Override
    public boolean isWhitelisted(String ipAddress) {
        return ipWhitelistRepository.findByIpAddress(ipAddress)
                .map(IpWhitelist::getIsActive)
                .orElse(false);
    }

    /**
     * Kiểm tra xem chế độ whitelist có đang được kích hoạt (có ít nhất một IP active) hay không.
     */
    @Override
    public boolean hasWhitelistEnabled() {
        return !ipWhitelistRepository.findByIsActiveTrue().isEmpty();
    }
}

