package com.frezo.crm.dto;

import lombok.Data;

import java.util.List;

@Data
public class EmailSequenceRequest {
    private String name;
    private String description;
    private Boolean active;
    private List<EmailSequenceStepRequest> steps;
}
