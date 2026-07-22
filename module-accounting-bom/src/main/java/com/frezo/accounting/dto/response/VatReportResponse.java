package com.frezo.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VatReportResponse {
    private Integer year;
    private Integer month;
    private Double outputVat;
    private Double inputVat;
    private Double netVat;
    private String standard;
    private String note;
}
