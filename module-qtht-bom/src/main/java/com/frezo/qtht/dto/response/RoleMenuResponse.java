package com.frezo.qtht.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleMenuResponse {
    private String roleId;
    private String menuId;
}