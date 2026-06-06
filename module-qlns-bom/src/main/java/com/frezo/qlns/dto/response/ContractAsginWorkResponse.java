package com.frezo.qlns.dto.response;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContractAsginWorkResponse {

    @Column(name = "contract_id")
    private String contractId;

    @Column(name = "assign_rv")
    private String assignRV; // người xét duyệt

    @Column(name = "assign_op")
    private String assignOP; // người thực hiện
}
