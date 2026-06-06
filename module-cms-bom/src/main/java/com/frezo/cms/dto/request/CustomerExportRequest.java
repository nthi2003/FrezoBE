package com.frezo.cms.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CustomerExportRequest {
    private String namePattern;
    private String status;
    private LocalDate fromDate;
    private LocalDate toDate;
}
