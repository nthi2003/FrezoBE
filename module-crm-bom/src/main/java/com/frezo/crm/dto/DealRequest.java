package com.frezo.crm.dto;

import com.frezo.crm.common.DealStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DealRequest {
    @NotBlank
    private String title;
    private String pipelineId;
    private String stageId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private Integer probability;
    private LocalDate expectedCloseDate;
    private DealStatus status;
    private String ownerUsername;
    private String description;
    private String lostReason;
}
