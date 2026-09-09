package com.frezo.qtbv.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tài sản của công ty — laptop, bàn ghế, xe, máy in, ...
 * <p>
 * Design goals:
 * <ul>
 *   <li>Phân loại linh hoạt qua {@code categoryCode} — không hardcode enum để admin
 *       tự thêm loại mới qua trang Danh mục.</li>
 *   <li>Track vòng đời qua {@code status}: AVAILABLE → IN_USE → MAINTENANCE → ...</li>
 *   <li>{@code assignedPersonId} là cache "ai đang giữ" — lịch sử đầy đủ ở
 *       {@link AssetAssignment} (append-only log).</li>
 *   <li>{@code purchasePrice} + {@code currentValue}: cho phép giả lập depreciation
 *       theo linear (kế toán VN thường 3-5 năm cho IT, 8-10 năm cho furniture).</li>
 * </ul>
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "asset", indexes = {
        @Index(name = "idx_asset_code", columnList = "code", unique = true),
        @Index(name = "idx_asset_status", columnList = "status"),
        @Index(name = "idx_asset_assigned", columnList = "assigned_person_id"),
        @Index(name = "idx_asset_category", columnList = "category_code"),
})
public class Asset extends BaseEntity {

    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "category_code", length = 50)
    private String categoryCode;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "model", length = 100)
    private String model;


    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_price", precision = 18, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "current_value", precision = 18, scale = 2)
    private BigDecimal currentValue;

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "assigned_person_id", length = 36)
    private String assignedPersonId;

    @Column(name = "assigned_at")
    private LocalDate assignedAt;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "note", length = 1000)
    private String note;
}
