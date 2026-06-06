package com.frezo.qtht.dto.response;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IpTrustResponse extends BaseEntity {
    @Column(name = "is_trust")
    private Boolean isTrust;

    @Column(name = "ip_number")
    private String ipNumber;

    @Column(name = "ip_name")
    private String ipName; // đánh dấu ip nhanh để dễ search
}
