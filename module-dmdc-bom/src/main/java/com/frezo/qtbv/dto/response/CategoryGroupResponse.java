package com.frezo.qtbv.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.frezo.qtbv.entity.Category;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
public class CategoryGroupResponse {
    private String code;

    private String name;

    private Integer catGroup;

    private Boolean isDeleted;

    private List<Category> categories;
}
