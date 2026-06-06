package com.frezo.customer.dto.request;

import com.frezo.common.model.PagingBase;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerFilterRequest extends PagingBase {
    private String keyword;
    private String type;
    private String status;
    private String categoryCode;
}
