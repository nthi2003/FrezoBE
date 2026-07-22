package com.frezo.crm.dto;

import lombok.Data;

@Data
public class EmailSequenceStepRequest {
    private Integer stepOrder;
    private Integer delayDays;
    private String subject;
    private String bodyHtml;
}
