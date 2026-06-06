package com.frezo.qlns.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ContractSnapshotRequest {
    private String id;
    private String code;
    private String name;
    private String personId;
    private String typeContractId;
    private LocalDate effTo;
    private LocalDate effFrom;
    private Integer value;
    private String status;
    private Boolean activated;
    private String htmlContract;
}
