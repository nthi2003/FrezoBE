package com.frezo.qlns.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload dùng chung cho tạo & cập nhật Requisition.
 * Trường {@code status} bỏ qua khi tạo mới (server ép về OPEN).
 */
@Data
public class RequisitionRequest {

    private String title;
    private String departmentId;
    private Integer quantity;
    private String level;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String hiringManagerUsername;
    private LocalDate openDate;
    private LocalDate closeDate;
    private String description;
    private String requirements;
    /** Chỉ dùng cho update; create luôn ép OPEN. */
    private String status;
}
