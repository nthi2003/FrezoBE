package com.frezo.qtbv.dto.request;

import com.frezo.common.model.PagingBase;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.awt.print.Pageable;

@Data
@RequiredArgsConstructor
public class CategoryFilter extends PagingBase {
    private String keyword;
    private Boolean active;
    private String type;
}
