package com.frezo.qtht.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleUpdateRequest {
    private String id;
    private String code;
    @JsonProperty("appCode")
    private String appCode;
    private String name;
    private String description;
    private String status;
}
