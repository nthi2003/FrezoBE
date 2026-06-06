package com.frezo.customer.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NCCCertificateDTO {
    private String id;
    private String nccId;
    private String certificateType;
    private String fileUrl;
    private LocalDate expiryDate;
}
