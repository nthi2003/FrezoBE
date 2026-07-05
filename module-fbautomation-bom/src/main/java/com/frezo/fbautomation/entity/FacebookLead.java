package com.frezo.fbautomation.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fb_leads")
public class FacebookLead extends BaseEntity {

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "source_group_id", length = 100)
    private String sourceGroupId;

    @Column(name = "source_group_name", length = 500)
    private String sourceGroupName;

    @Column(name = "profile_url", length = 1000)
    private String profileUrl;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "imported_customer_id", length = 36)
    private String importedCustomerId;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
