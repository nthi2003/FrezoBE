package com.frezo.qtbv.dto.response;

import com.frezo.qtht.common.OrganizationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrganizationSimpleResponse {
    private String id;
    private String code;
    private String name;
    private String shortName;
    private String email;
    private String phone;
    private OrganizationStatus status;
}
