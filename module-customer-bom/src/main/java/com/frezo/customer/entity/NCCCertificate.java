package com.frezo.customer.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ncc_certificates")
public class NCCCertificate extends BaseEntity {

    @Column(name = "ncc_id", length = 50, nullable = false)
    private String nccId;

    @Column(name = "certificate_type", length = 50)
    private String certificateType; // VietGAP, GlobalGAP, Organic, VSATTP

    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;
}
