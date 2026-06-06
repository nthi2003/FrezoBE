package com.frezo.qlns.dto.response;

import com.frezo.qlns.common.StatusContarct;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ContractComboboxResponse {
    private String id;

    private String name;

    private String personId;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false, unique = true)
    private String tyContractId;

    private LocalDate effTo ; // Ngày hiệu lực

    private LocalDate effFrom; // Ngày hết hợp đồng

    private Integer value;

    private StatusContarct status;

    private Boolean activated;
}
