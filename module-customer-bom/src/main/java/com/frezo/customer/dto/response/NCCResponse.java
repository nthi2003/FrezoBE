package com.frezo.customer.dto.response;

import com.frezo.customer.dto.NCCCertificateDTO;
import lombok.Data;

import java.util.List;

@Data
public class NCCResponse {
    private String id;
    private String name;
    private String code;
    private String representative;
    private String phone;
    private String address;
    private String classificationCode; // groupCode = "PhanLoaiNCC" ăn ở agnecy THINVQ
    private String classificationName;
    private Double growingArea;
    private Double maxCapacity;
    private String strengths;
    private List<NCCCertificateDTO> certificates;
}
