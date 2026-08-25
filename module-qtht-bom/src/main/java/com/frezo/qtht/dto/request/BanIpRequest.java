package com.frezo.qtht.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BanIpRequest {

    private String ipAddress;
    private String reason;
    @Builder.Default
    private String bannedBy = "SYSTEM";
    private Integer hours;
}
