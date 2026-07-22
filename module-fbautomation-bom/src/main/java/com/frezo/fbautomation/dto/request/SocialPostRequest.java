package com.frezo.fbautomation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class SocialPostRequest {

    @Size(max = 36, message = "orgId tối đa 36 ký tự")
    private String orgId;

    /** FACEBOOK_PAGE | ZALO_OA | INSTAGRAM | TIKTOK. */
    @NotBlank(message = "Vui lòng chọn kênh đăng")
    @Size(max = 32)
    private String channel;

    @Size(max = 128) private String targetId;
    @Size(max = 255) private String targetName;
    @Size(max = 500) private String title;

    /** Nội dung bài viết (không rỗng khi lưu bản nháp). */
    @Size(max = 20000, message = "Nội dung tối đa 20.000 ký tự")
    private String content;

    /** JSON string chứa array URL media. */
    private String mediaUrls;

    @Size(max = 1000) private String linkUrl;

    /** Nếu truyền `scheduledAt` = post sẽ ở trạng thái SCHEDULED. Bỏ trống → DRAFT. */
    private OffsetDateTime scheduledAt;
}
