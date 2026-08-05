package com.frezo.fbautomation.service;

import com.frezo.fbautomation.dto.request.SocialPostRequest;
import com.frezo.fbautomation.dto.response.SocialPostResponse;

import java.util.List;

/**
 * SocialPostService — CRUD bài viết + scheduler runner.
 * <p>
 * Turn này chỉ implement CRUD + queue lên lịch. Khi nào có Meta Page Token (App Review
 * xong), {@code SocialPostSchedulerRunner} sẽ publish thật lên Graph API `POST /{page-id}/feed`.
 * Trước đó, tới giờ scheduled_at → chỉ đánh dấu "cần đăng" + gửi notification cho user
 * để đăng thủ công.
 */
public interface SocialPostService {

    SocialPostResponse create(SocialPostRequest req);

    SocialPostResponse update(String id, SocialPostRequest req);

    SocialPostResponse get(String id);

    void delete(String id);

    /** Duplicate bài viết — tiện tạo variant A/B testing. */
    SocialPostResponse duplicate(String id);

    /** Cancel bài SCHEDULED. */
    SocialPostResponse cancel(String id);

    /** Publish ngay lập tức (nếu có Page Token). */
    SocialPostResponse publishNow(String id);

    /** List with basic filter. */
    List<SocialPostResponse> list(String status, String channel);

    /**
     * Quét bài SCHEDULED tới giờ và publish. Lịch chạy do bảng {@code system_job} quyết định —
     * gọi qua {@code SocialPostPublishJob}.
     */
    void runScheduler();
}
