package com.frezo.product.dto.request;

import lombok.Data;

@Data
public class ProductUpdateRequest {
    private String name;
    private Double price;
    private String imageUrl;
    private String category;
    private Double rating;
    private Boolean isNew;
    private String description;
    private Boolean isActive;
}
