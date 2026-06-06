package com.frezo.customer.dto.request;

import com.frezo.customer.dto.NCCCertificateDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CustomerRequest {

    @NotBlank(message = "Tên khách hàng không được để trống")
    private String name;

    private String code;
    private String phone;
    private String email;
    private String address;
    private String taxCode;
    private String type;    // INDIVIDUAL | COMPANY
    private String status;  // ACTIVE | INACTIVE
    private String categoryCode;
    private String note;
}
