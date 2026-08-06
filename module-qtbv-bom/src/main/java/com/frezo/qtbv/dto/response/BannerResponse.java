package com.frezo.qtbv.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerResponse {
    private String id;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String linkUrl;
    private String position;
    private String status;
    private Integer orderIndex;
    private String organizationId;
    private Boolean pinForNewsPage;
}
