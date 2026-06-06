package com.frezo.qtbv.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoryGroupChildrenResponse {
    private String code;
    private String name;
}