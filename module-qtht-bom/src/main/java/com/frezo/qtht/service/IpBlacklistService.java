package com.frezo.qtht.service;

import com.frezo.qtht.dto.request.BanIpRequest;
import com.frezo.qtht.entity.IpBlacklist;
import java.util.List;

public interface IpBlacklistService {
    IpBlacklist addBan(String ip, String reason, String bannedBy, Integer hours);
    IpBlacklist addBan(BanIpRequest request);
    IpBlacklist addBanMinutes(String ip, String reason, String bannedBy, Integer minutes);
    void unban(String id);
    List<IpBlacklist> getAllActiveBans();
    List<String> getAllActiveBanIps();
    boolean isBanned(String ip);
}
