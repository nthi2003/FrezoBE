package com.frezo.qlns.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationResponse {

    private String id;
    private String candidateId;
    private String candidateName;
    private String requisitionId;
    private String requisitionTitle;
    private String stage;
    private LocalDate appliedDate;
    private String currentAssignee;
    private String rejectionReason;
}
