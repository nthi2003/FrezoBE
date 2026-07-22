package com.frezo.crm.dto;

import com.frezo.crm.common.LeadStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LeadRequest {
    @NotBlank
    private String fullName;
    private String phone;
    private String email;
    private String companyName;
    private String source;
    private LeadStatus status;
    private Integer score;
    private String ownerUsername;
    private String description;
}
