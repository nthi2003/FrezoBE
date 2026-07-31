package com.frezo.qlns.dto.request;

import com.frezo.common.model.PagingBase;
import com.frezo.qlns.common.StatusContarct;
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
    /** Lọc theo nhân sự — leave/payroll combobox. */
    private String personId;
    /** Lọc theo trạng thái HĐ (vd. ACTIVE). Entity field name = Status. */
    private StatusContarct status;
}
