package com.frezo.qlns.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TokenTransferResponse {
    private String id;
    private String fromPersonId;
    private String fromPersonName;
    private String toPersonId;
    private String toPersonName;
    private BigDecimal amount;
    private String note;
    private String sourceType;
    private String sourceId;
    private String createdDate;
    private String createdBy;
}
