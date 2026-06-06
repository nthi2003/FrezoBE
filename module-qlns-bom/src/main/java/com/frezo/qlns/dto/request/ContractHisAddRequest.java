package com.frezo.qlns.dto.request;

import com.frezo.qlns.common.StatusContarct;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContractHisAddRequest {
    private String contractId;
    private StatusContarct statusContarct;
    private String versionId;
}
