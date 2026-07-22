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

    /** Nguồn Lead (FB / Google / Referral / Website / Import...). */
    @Column(length = 50)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LeadStatus status;

    /** Score 0-100 (đánh giá độ tiềm năng). */
    @Column(name = "score")
    private Integer score;

    /** Sales phụ trách. */
    @Column(name = "owner_username", length = 50)
    private String ownerUsername;

    /** Nếu đã convert, ghi customer_id vừa tạo. */
    @Column(name = "converted_customer_id", length = 36)
    private String convertedCustomerId;

    /** Nếu đã convert, ghi deal_id vừa tạo. */
    @Column(name = "converted_deal_id", length = 36)
    private String convertedDealId;

    @Column(length = 2000)
    private String description;
}
