package com.frezo.qtht.service.impl;

import com.frezo.qtht.entity.IpBlacklist;
import com.frezo.qtht.repository.IpBlacklistRepository;
import com.frezo.qtht.service.IpBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IpBlacklistServiceImpl implements IpBlacklistService {

    private final IpBlacklistRepository ipBlacklistRepository;

    @Override
    public IpBlacklist addBan(String ip, String reason, String bannedBy, Integer hours) {
        LocalDateTime bannedUntil = (hours != null) ? LocalDateTime.now().plusHours(hours) : null;
        IpBlacklist ban = IpBlacklist.builder()
                .ipAddress(ip)
                .reason(reason)
                .bannedBy(bannedBy)
                .bannedUntil(bannedUntil)
                .active(true)
                .build();
        ban.setIsDeleted(false);
        return ipBlacklistRepository.save(ban);
    }

    @Override
    public IpBlacklist addBanMinutes(String ip, String reason, String bannedBy, Integer minutes) {
        LocalDateTime bannedUntil = (minutes != null) ? LocalDateTime.now().plusMinutes(minutes) : null;
        IpBlacklist ban = IpBlacklist.builder()
                .ipAddress(ip)
                .reason(reason)
                .bannedBy(bannedBy)
                .bannedUntil(bannedUntil)
                .active(true)
                .build();
        ban.setIsDeleted(false);
        return ipBlacklistRepository.save(ban);
    }

    @Override
    public void unban(String id) {
        ipBlacklistRepository.findById(id).ifPresent(ban -> {
            ban.setActive(false);
            ban.setBannedUntil(LocalDateTime.now());
            ipBlacklistRepository.save(ban);
        });
    }

    @Override
    public List<IpBlacklist> getAllActiveBans() {
        LocalDateTime now = LocalDateTime.now();
        return ipBlacklistRepository.findAll().stream()
                .filter(i -> Boolean.TRUE.equals(i.getActive()) || (i.getBannedUntil() != null && i.getBannedUntil().isAfter(now)))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isBanned(String ip) {
        return ipBlacklistRepository.findActiveBanByIp(ip, LocalDateTime.now()).isPresent();
    }
}
