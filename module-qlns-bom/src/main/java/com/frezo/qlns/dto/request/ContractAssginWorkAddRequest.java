package com.frezo.qlns.dto.request;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContractAssginWorkAddRequest {

    @Column(name = "contract_id")
    private String contractId;

    @Column(name = "assign_rv")
    private String assignRV; // người xét duyệt

    @Column(name = "assign_op")
    private String assignOP; // người thực hiện
}
