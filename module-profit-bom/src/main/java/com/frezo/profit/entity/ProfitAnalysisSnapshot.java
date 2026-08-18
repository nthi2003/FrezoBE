package com.frezo.profit.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Immutable audit snapshot of profit analysis — EU-ready audit trail (GDPR: no PII in JSON beyond person IDs).
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "profit_analysis_snapshot",
        indexes = @Index(name = "idx_profit_snapshot_period", columnList = "year, month"))
public class ProfitAnalysisSnapshot extends BaseEntity {

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "calculated_by", length = 50)
    private String calculatedBy;

    @Lob
    @Column(name = "input_json")
    private String inputJson;

    @Lob
    @Column(name = "result_json")
    private String resultJson;
}
