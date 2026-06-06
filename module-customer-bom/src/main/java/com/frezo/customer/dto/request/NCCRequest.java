package com.frezo.customer.dto.request;

import com.frezo.customer.dto.NCCCertificateDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class NCCRequest {
    @NotBlank(message = "Tên NCC không được để trống")
    private String name;

    private String code;

    private String representative;
    private String phone;
    private String address;
    private String classificationCode;
    private Double growingArea;
    private Double maxCapacity;
    private String strengths;
    private List<NCCCertificateDTO> certificates;
}
