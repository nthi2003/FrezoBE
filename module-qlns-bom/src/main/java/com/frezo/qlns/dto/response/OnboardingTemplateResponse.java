package com.frezo.qlns.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingTemplateResponse {
    private String id;
    private String name;
    private String description;
    private Boolean active;
    private List<OnboardingTemplateItemResponse> items;
}
