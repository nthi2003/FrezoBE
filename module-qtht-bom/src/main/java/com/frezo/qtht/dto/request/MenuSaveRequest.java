package com.frezo.qtht.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class MenuSaveRequest {
    private String code;
    @JsonProperty("appCode")
    private String appCode;

    @NotBlank(message = "menu.name.not_blank")
    private String name;

    private String nameEn;

    @JsonProperty("parentCode")
    private String parentCode;
    private String api;
    private String icon;
    @JsonProperty("orderIndex")
    private Integer orderIndex;
    @JsonProperty("menuType")
    private Short menuType;
    @JsonProperty("isPublic")

    private Boolean isPublic = false;
    private Boolean status = true;
    @JsonProperty("feUrl")
    private String feUrl;
    @JsonProperty("folderPath")
    private String folderPath;

    @JsonProperty("permissionIds")
    private java.util.List<String> permissionIds;
}