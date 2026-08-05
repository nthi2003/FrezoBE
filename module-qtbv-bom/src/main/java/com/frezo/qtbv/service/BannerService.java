package com.frezo.qtbv.service;

import com.frezo.qtbv.entity.Banner;

import java.util.List;

public interface BannerService {
    List<Banner> findAll();

    Banner findById(String id);

    Banner create(Banner request);

    Banner update(String id, Banner request);

    void delete(String id);
}
