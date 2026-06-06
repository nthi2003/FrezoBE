package com.frezo.product.dto.request;

import lombok.Data;

@Data
public class ProductFilterRequest {
    private String keyword;
    private String categoryId;
    private Boolean isActive;
    private Integer page = 0;
    private Integer size = 10;
}
