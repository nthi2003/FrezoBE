package com.frezo.qlns.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResignationHandoverRequest {

    private Boolean laptopReturned;
    private Boolean badgeReturned;
    private Boolean docsHandedOver;
    private String note;
}
