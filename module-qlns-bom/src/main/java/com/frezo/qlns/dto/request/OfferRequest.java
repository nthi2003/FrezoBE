package com.frezo.qlns.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class OfferRequest {

    private String applicationId;
    private BigDecimal offeredSalary;
    private LocalDate startDate;
    private LocalDateTime expiresAt;
    private String notes;
}
