package com.frezo.qlns.dto.response;

import com.frezo.qlns.common.StatusContarct;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ContractResponse {

    private String id;

    private String name;

    private String personId;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false, unique = true)
    private String typeContractId;

    private LocalDate effTo ; // Ngày hiệu lực

    private LocalDate effFrom; // Ngày hết hợp đồng

    private Integer value;

    private StatusContarct Status;

    private Boolean activated;

    private String assignRV; // người xét duyệt

    private String assignOP; // người thực hiện

}
