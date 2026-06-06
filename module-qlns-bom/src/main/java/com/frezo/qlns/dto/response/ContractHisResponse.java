package com.frezo.qlns.dto.response;

import com.frezo.qlns.common.StatusContarct;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ContractHisResponse {

    private String id;
    private String contractId;
    private StatusContarct statusContarct;
    private String versionId;
    private String createdBy;
    private LocalDateTime createdDate;
    private String updatedBy;
    private LocalDateTime updatedDate;
}
