package com.frezo.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiRequest {
    private String functionCode;
    private String actionCode;
    private Object data;
}
