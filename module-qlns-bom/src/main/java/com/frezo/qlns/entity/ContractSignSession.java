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

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "contract_sign_session", indexes = {
        @Index(name = "idx_contract_sign", columnList = "contract_id")
})
public class ContractSignSession extends BaseEntity {

    @Column(name = "contract_id", length = 36, nullable = false)
    private String contractId;

    @Column(name = "otp_hash", length = 128, nullable = false)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "signed_by", length = 100)
    private String signedBy;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "device", length = 255)
    private String device;

    /** PENDING / SIGNED / EXPIRED */
    @Column(name = "status", length = 20)
    private String status;
}
