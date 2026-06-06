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

@Service
@RequiredArgsConstructor
@Slf4j
public class IpWhitelistServiceImpl implements IpWhitelistService {

    private final IpWhitelistRepository ipWhitelistRepository;

    @Override
    public List<IpWhitelist> getAllActive() {
        return ipWhitelistRepository.findByIsActiveTrue();
    }

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

    @Override
    @Transactional
    public void removeFromWhitelist(String id) {
        IpWhitelist whitelist = ipWhitelistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("IP không tồn tại trong whitelist"));
        whitelist.setIsActive(false);
        ipWhitelistRepository.save(whitelist);
    }

    @Override
    public boolean isWhitelisted(String ipAddress) {
        return ipWhitelistRepository.findByIpAddress(ipAddress)
                .map(IpWhitelist::getIsActive)
                .orElse(false);
    }

    @Override
    public boolean hasWhitelistEnabled() {
        return !ipWhitelistRepository.findByIsActiveTrue().isEmpty();
    }
}

