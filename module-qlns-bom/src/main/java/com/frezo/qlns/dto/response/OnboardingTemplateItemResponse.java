package com.frezo.qlns.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingTemplateItemResponse {
    private String id;
    private String title;
    private String description;
    private String assigneeRole;
    private Integer dueDayOffset;
    private Integer sortOrder;
    private Boolean required;
}
