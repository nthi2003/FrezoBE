package com.frezo.qtbv.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Lịch sử cấp phát / thu hồi / bảo trì tài sản — append-only.
 * <p>
 * Mỗi lần asset đổi chủ / đi bảo trì / thanh lý → 1 row mới. Không update row cũ
 * để giữ audit trail đầy đủ (kế toán yêu cầu).
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "asset_assignment", indexes = {
        @Index(name = "idx_aa_asset", columnList = "asset_id"),
        @Index(name = "idx_aa_person", columnList = "person_id"),
})
public class AssetAssignment extends BaseEntity {

    @Column(name = "asset_id", length = 36, nullable = false)
    private String assetId;
    @Column(name = "action", length = 30, nullable = false)
    private String action;
    @Column(name = "person_id", length = 36)
    private String personId;
    @Column(name = "person_name", length = 255)
    private String personName;
    @Column(name = "action_date", nullable = false)
    private LocalDate actionDate;
    @Column(name = "note", length = 1000)
    private String note;
    @Column(name = "cost", precision = 18, scale = 2)
    private java.math.BigDecimal cost;
}
