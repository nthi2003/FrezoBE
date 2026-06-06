package com.frezo.product.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductResponse {
    private String id;
    private String name;
    private Double price;
    private String imageUrl;
    private String category;
    private Double rating;
    private Boolean isNew;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdDate;
}
