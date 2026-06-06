package com.frezo.qtbv.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoryResponse {
    private String id;
    private String code;
    private String name;
    private String nameEn;
    private String shortName;
    private String parentCode;
    private String groupCode;
    private Integer orderIndex;
    private LocalDate effectFromDate;
    private LocalDate effectToDate;
    private CategoryGroupChildrenResponse categoryGroup;
    private List<CategoryResponse> children;
    private Boolean activated;
    private String description;
    private LocalDateTime updatedDate;
    private LocalDateTime createdDate;
}
