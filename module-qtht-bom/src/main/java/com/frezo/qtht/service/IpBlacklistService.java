package com.frezo.qtht.service;

import com.frezo.qtht.entity.IpBlacklist;
import java.util.List;

public interface IpBlacklistService {
    IpBlacklist addBan(String ip, String reason, String bannedBy, Integer hours);
    IpBlacklist addBanMinutes(String ip, String reason, String bannedBy, Integer minutes);
    void unban(String id);
    List<IpBlacklist> getAllActiveBans();
    boolean isBanned(String ip);
}
