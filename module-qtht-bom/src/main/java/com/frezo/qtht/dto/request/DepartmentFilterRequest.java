package com.frezo.qtht.dto.request;

import com.frezo.common.model.PagingBase;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentFilterRequest extends PagingBase {
    public String keyword;
    private Boolean activated;
    private String managerId;
    private String organizationId;

}
