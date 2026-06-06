package com.frezo.customer.dto.response;

import lombok.Data;

@Data
public class CustomerResponse {
    private String id;
    private String name;
    private String code;
    private String phoneLast4;   // hiển thị 4 số cuối (****1234)
    private String email;
    private String address;
    private String taxCode;
    private String type;
    private String status;
    private String categoryCode;
    private String note;
    private String createdBy;
    private String createdDate;
}
