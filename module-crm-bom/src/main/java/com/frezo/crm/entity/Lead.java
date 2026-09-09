package com.frezo.crm.entity;

import com.frezo.common.domain.BaseEntity;
import com.frezo.crm.common.LeadStatus;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "crm_lead", indexes = {
        @Index(name = "idx_crm_lead_status", columnList = "status"),
        @Index(name = "idx_crm_lead_owner", columnList = "owner_username")
})
public class Lead extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(length = 50)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LeadStatus status;


    @Column(name = "score")
    private Integer score;

    @Column(name = "owner_username", length = 50)
    private String ownerUsername;


    @Column(name = "converted_customer_id", length = 36)
    private String convertedCustomerId;


    @Column(name = "converted_deal_id", length = 36)
    private String convertedDealId;

    @Column(length = 2000)
    private String description;
}
