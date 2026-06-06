package com.frezo.email.dto.request;

import com.frezo.common.model.PagingBase;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EmailTemplateFilter extends PagingBase {
    private String keyword;
}
