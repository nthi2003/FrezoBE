package com.frezo.qlns.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "okr_key_result", indexes = @Index(name = "idx_okr_kr_okr", columnList = "okr_id"))
public class OkrKeyResult extends BaseEntity {

    @Column(name = "okr_id", length = 36, nullable = false)
    private String okrId;

    @Column(name = "title", length = 500, nullable = false)
    private String title;

    @Column(name = "target_value")
    private Double targetValue;

    @Column(name = "current_value")
    private Double currentValue;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "progress")
    private Double progress;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
