package com.frezo.qtht.service;

import com.frezo.qtht.dto.request.GuideSaveRequest;
import com.frezo.qtht.dto.response.GuideResponse;
import com.frezo.qtht.dto.response.GuideSummaryResponse;

import java.util.List;

public interface GuideService {

    List<GuideSummaryResponse> listPublished();

    GuideResponse getPublishedBySlug(String slug);

    List<GuideSummaryResponse> listAll();

    GuideResponse getById(String id);

    GuideResponse create(GuideSaveRequest request);

    GuideResponse update(String id, GuideSaveRequest request);

    GuideResponse publish(String id);

    GuideResponse unpublish(String id);

    void delete(String id);
}
