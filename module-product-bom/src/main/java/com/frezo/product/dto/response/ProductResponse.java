package com.frezo.product.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductResponse {
    private String id;
    private String code;
    private String name;
    private Double price;
    private String imageUrl;
    private String category;   // alias của categoryId — cho FE dễ dùng
    private String categoryId; // giữ nguyên để filter/join
    private String origin;
    private String season;
    private Double warningThreshold;
    private Integer expiryAlertDays;
    private Double rating;
    private Boolean isNew;
    private String description;
    private Boolean isActive;
    private Long viewCount;
    private LocalDateTime createdDate;
}
