package com.frezo.qtht.dto.request;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleMenuAssignRequest {
    private String roleCode;
    private String api;
    private String action;
    private String appCode;
}
