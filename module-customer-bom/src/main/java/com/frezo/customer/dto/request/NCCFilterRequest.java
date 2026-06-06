package com.frezo.customer.dto.request;

import lombok.Data;

@Data
public class NCCFilterRequest {
    private String keyword;
    private String classificationCode;
}
