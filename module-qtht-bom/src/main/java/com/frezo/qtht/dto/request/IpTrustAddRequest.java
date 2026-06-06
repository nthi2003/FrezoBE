package com.frezo.qtht.dto.request;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IpTrustAddRequest {
    @Column(name = "is_trust")
    private Boolean isTrust;

    @Column(name = "ip_number")
    private String ipNumber;

    @Column(name = "ip_name")
    private String ipName; // đánh dấu ip nhanh để dễ search
}
