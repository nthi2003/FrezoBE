package com.frezo.qtht.dto.response;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {
    private String id;
    private String code;
    private String appCode;
    private String name;
    private String description;
    private String status;
}