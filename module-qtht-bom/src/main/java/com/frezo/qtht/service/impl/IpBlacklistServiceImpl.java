package com.frezo.qtht.service.impl;

import com.frezo.qtht.dto.request.BanIpRequest;
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

    /**
     * Thêm một lệnh cấm IP theo số giờ, tự động hết hạn sau khoảng thời gian đã cho.
     */
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

    /**
     * Thêm lệnh cấm IP từ đối tượng yêu cầu BanIpRequest.
     */
    @Override
    public IpBlacklist addBan(BanIpRequest request) {
        return addBan(request.getIpAddress(), request.getReason(),
                request.getBannedBy(), request.getHours());
    }

    /**
     * Thêm một lệnh cấm IP theo số phút, tự động hết hạn sau khoảng thời gian đã cho.
     */
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

    /**
     * Gỡ lệnh cấm IP theo mã định danh, đặt trạng thái active = false và hết hạn ngay.
     */
    @Override
    public void unban(String id) {
        ipBlacklistRepository.findById(id).ifPresent(ban -> {
            ban.setActive(false);
            ban.setBannedUntil(LocalDateTime.now());
            ipBlacklistRepository.save(ban);
        });
    }

    /**
     * Lấy danh sách tất cả lệnh cấm IP đang hoạt động (active hoặc chưa hết hạn).
     */
    @Override
    public List<IpBlacklist> getAllActiveBans() {
        LocalDateTime now = LocalDateTime.now();
        return ipBlacklistRepository.findAll().stream()
                .filter(i -> Boolean.TRUE.equals(i.getActive()) || (i.getBannedUntil() != null && i.getBannedUntil().isAfter(now)))
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách địa chỉ IP đang bị cấm (chỉ trả về chuỗi IP).
     */
    @Override
    public List<String> getAllActiveBanIps() {
        return getAllActiveBans().stream()
                .map(IpBlacklist::getIpAddress)
                .collect(Collectors.toList());
    }

    /**
     * Kiểm tra xem một địa chỉ IP có đang bị cấm hay không.
     */
    @Override
    public boolean isBanned(String ip) {
        return ipBlacklistRepository.findActiveBanByIp(ip, LocalDateTime.now()).isPresent();
    }
}
