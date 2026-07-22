package com.frezo.qlns.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class OnboardingTemplateRequest {
    private String name;
    private String description;
    private Boolean active;
    private List<OnboardingTemplateItemRequest> items;
}
