package com.frezo.warehouse.dto.request;

import lombok.Data;
import java.time.LocalDate;

/** Cập nhật metadata GRN khi còn DRAFT / PENDING_APPROVAL / APPROVED. */
@Data
public class GrnUpdateRequest {
    private String invoiceNo;
    private LocalDate invoiceDate;
    private String note;
}
