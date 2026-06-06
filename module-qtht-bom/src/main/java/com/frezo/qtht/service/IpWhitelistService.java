package com.frezo.qtht.service;

import com.frezo.qtht.entity.IpWhitelist;

import java.util.List;

public interface IpWhitelistService {

    List<IpWhitelist> getAllActive();

    IpWhitelist addToWhitelist(String ipAddress, String description, String createdBy);

    void removeFromWhitelist(String id);

    boolean isWhitelisted(String ipAddress);

    boolean hasWhitelistEnabled();
}

