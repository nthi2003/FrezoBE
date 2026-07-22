package com.frezo.qlns.dto.request;

import lombok.Data;

@Data
public class OnboardingTemplateItemRequest {
    private String title;
    private String description;
    private String assigneeRole;
    private Integer dueDayOffset;
    private Integer sortOrder;
    private Boolean required;
}
