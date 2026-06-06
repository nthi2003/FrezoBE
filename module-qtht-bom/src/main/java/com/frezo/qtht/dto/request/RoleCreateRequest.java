package com.frezo.qtht.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleCreateRequest {
    private String code;
    @JsonProperty("appCode")
    private String appCode;
    private String name;
    private String description;
}
