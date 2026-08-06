package com.frezo.qtbv.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewsCategoryRequest {
    private String name;
    private String color;
    private String organizationId;
    private Integer orderIndex;
}
