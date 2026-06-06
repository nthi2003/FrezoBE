package com.frezo.product.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(name = "code", unique = true, nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "origin")
    private String origin; // Nguồn gốc (Đà Lạt, Long An...)

    @Column(name = "season")
    private String season; // Mùa vụ (Đông Xuân, Hè Thu...)

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "category_id")
    private String categoryId;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "warning_threshold")
    private Double warningThreshold; // Ngưỡng cảnh báo hết hàng

    @Column(name = "expiry_alert_days")
    private Integer expiryAlertDays; // Cảnh báo trước bao nhiêu ngày khi sắp hỏng

    @Column(name = "is_active")
    private Boolean isActive = true;
}
