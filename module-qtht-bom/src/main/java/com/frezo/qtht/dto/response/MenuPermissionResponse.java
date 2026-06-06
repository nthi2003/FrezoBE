package com.frezo.qtht.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuPermissionResponse {
    private String id;
    private String menuId;
    private String name;
    private String code;
    private String apiPath;
    private String method;
    private String action;
}
