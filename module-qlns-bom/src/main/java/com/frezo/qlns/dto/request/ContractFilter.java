package com.frezo.qlns.dto.request;

import com.frezo.common.model.PagingBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class ContractFilter extends PagingBase {
    private String keyword;
    private LocalDate effTo;
    private LocalDate effFrom;
    private Boolean isDelete;
}
