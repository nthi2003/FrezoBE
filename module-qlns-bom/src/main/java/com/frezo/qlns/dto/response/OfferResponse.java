package com.frezo.qlns.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferResponse {

    private String id;
    private String applicationId;
    private BigDecimal offeredSalary;
    private LocalDate startDate;
    private LocalDateTime expiresAt;
    private String status;
    private String notes;
    private LocalDateTime sentAt;
    private LocalDateTime respondedAt;
}
