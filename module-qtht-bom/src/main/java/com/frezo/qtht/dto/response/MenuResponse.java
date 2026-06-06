package com.frezo.qtht.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuResponse {

    private String id;
    private String code;
    private String appCode;
    private String name;
    private String nameEn;
    private String parentCode;
    private Integer orderIndex;
    private Short menuType;
    private Boolean isPublic;
    private Boolean status;
    private String icon;

    @JsonProperty("feUrl")
    private String feUrl;

    @JsonProperty("folderPath")
    private String folderPath;

    private List<MenuPermissionResponse> permissions;
}
