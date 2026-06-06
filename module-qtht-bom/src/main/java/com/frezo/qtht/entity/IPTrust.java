package com.frezo.qtht.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ip_trust")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IPTrust extends BaseEntity {

    @Column(name = "is_trust")
    private Boolean isTrust;

    @Column(name = "ip_number")
    private String ipNumber;

    @Column(name = "ip_name")
    private String ipName; // đánh dấu ip nhanh để dễ search


}
