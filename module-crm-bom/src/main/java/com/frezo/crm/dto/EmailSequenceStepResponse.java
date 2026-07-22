package com.frezo.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSequenceStepResponse {
    private String id;
    private Integer stepOrder;
    private Integer delayDays;
    private String subject;
    private String bodyHtml;
}
