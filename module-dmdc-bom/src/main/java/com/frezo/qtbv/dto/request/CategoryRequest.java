package com.frezo.qtbv.dto.request;

import com.frezo.qtbv.entity.CategoryGroup;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@RequiredArgsConstructor
public class CategoryRequest {

    private String code;
    private String name;
    private String nameEn;
    private String shortName;
    private String parentCode;
    private String groupCode;
    private Integer orderIndex;
    private LocalDate effectFrom;
    private LocalDate effectTo;
    private CategoryGroup categoryGroup;
    private String createdBy;
    private String updatedBy;
    private Boolean activated;
    private String description;
}
