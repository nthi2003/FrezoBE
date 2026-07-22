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

    /** Mã tài sản — auto-gen dạng {@code AS-YYYY-####} (VD: {@code AS-2026-0007}). */
    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    /**
     * Danh mục — reference {@link Category#getCode()} với {@code groupCode='LoaiTaiSan'}.
     * Chuỗi (không FK cứng) để admin có thể xoá/rename danh mục mà không break asset cũ.
     * VD: {@code LAPTOP, DESK, CHAIR, CAR, PRINTER, MONITOR, PHONE, OTHER}.
     */
    @Column(name = "category_code", length = 50)
    private String categoryCode;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "model", length = 100)
    private String model;

    /** Số serial / IMEI — nên unique nhưng không bắt buộc (bàn ghế không có SN). */
    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_price", precision = 18, scale = 2)
    private BigDecimal purchasePrice;

    /** Giá trị còn lại sau khấu hao. Nếu null → dùng {@code purchasePrice}. */
    @Column(name = "current_value", precision = 18, scale = 2)
    private BigDecimal currentValue;

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    /**
     * AVAILABLE | IN_USE | MAINTENANCE | BROKEN | DISPOSED | LOST
     * <ul>
     *   <li>AVAILABLE — sẵn sàng cấp phát</li>
     *   <li>IN_USE — đang cấp phát cho ai đó (kèm {@code assignedPersonId})</li>
     *   <li>MAINTENANCE — đang bảo trì / sửa chữa</li>
     *   <li>BROKEN — hỏng, chờ thanh lý hoặc sửa</li>
     *   <li>DISPOSED — đã thanh lý / bán</li>
     *   <li>LOST — mất</li>
     * </ul>
     */
    @Column(name = "status", length = 30, nullable = false)
    private String status;

    /** Vị trí đặt — VD: "Tầng 3, phòng IT", hoặc mã kho. */
    @Column(name = "location", length = 255)
    private String location;

    /** ID {@code Person} đang được cấp phát. Null nếu chưa cấp. */
    @Column(name = "assigned_person_id", length = 36)
    private String assignedPersonId;

    /** Ngày cấp phát hiện tại (cho asset đang IN_USE). */
    @Column(name = "assigned_at")
    private LocalDate assignedAt;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "note", length = 1000)
    private String note;
}
