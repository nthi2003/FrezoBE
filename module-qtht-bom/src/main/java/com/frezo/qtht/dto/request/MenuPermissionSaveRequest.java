package com.frezo.qtht.dto.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class MenuPermissionSaveRequest {
    private String id;
    private String menuId;
    private String name;
    private String code;
    private String apiPath;
    private String method;
    private String action;
    private String appCode;

}
