package com.frezo.qtbv.dto.request;

import com.frezo.common.model.PagingBase;
import com.frezo.qtht.common.OrganizationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationFilterRequest extends PagingBase {
    private String name;
    private String code;
    private OrganizationStatus status;
    private String email;
}
