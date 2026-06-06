package com.frezo.qtht.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RolePermissionSaveRequest {
    @JsonProperty("roleId")
    private String roleId;
    @JsonProperty("appCode")
    private String appCode;
    @JsonProperty("menuIds")
    private List<String> menuIds;
}