package com.frezo.qlns.dto.request;

import lombok.Data;

@Data
public class JobApplicationRequest {

    private String candidateId;
    private String requisitionId;
    private String currentAssignee;
}
