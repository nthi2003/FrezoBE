package com.frezo.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSequenceResponse {
    private String id;
    private String name;
    private String description;
    private Boolean active;
    private List<EmailSequenceStepResponse> steps;
}
