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

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "token_transfer", indexes = {
        @Index(name = "idx_token_xfer_from", columnList = "from_person_id"),
        @Index(name = "idx_token_xfer_to", columnList = "to_person_id"),
        @Index(name = "idx_token_xfer_source", columnList = "source_type, source_id")
})
public class TokenTransfer extends BaseEntity {

    @Column(name = "from_person_id", length = 36, nullable = false)
    private String fromPersonId;

    @Column(name = "to_person_id", length = 36, nullable = false)
    private String toPersonId;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "note", length = 1000)
    private String note;

    /** MANUAL | TASK */
    @Column(name = "source_type", length = 30, nullable = false)
    private String sourceType;

    @Column(name = "source_id", length = 36)
    private String sourceId;
}
