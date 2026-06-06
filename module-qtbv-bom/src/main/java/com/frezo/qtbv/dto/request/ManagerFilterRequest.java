package com.frezo.qtbv.dto.request;

import com.frezo.common.model.PagingBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerFilterRequest extends PagingBase {
    private String name;
    private String code;
    private String email;
}
