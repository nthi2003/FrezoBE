package com.frezo.qlns.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequisitionResponse {

    private String id;
    private String title;
    private String departmentId;
    private Integer quantity;
    private String level;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String status;
    private String hiringManagerUsername;
    private LocalDate openDate;
    private LocalDate closeDate;
    private String description;
    private String requirements;
    /** Số application đã HIRED — server tính khi trả về, giúp FE biết còn slot không. */
    private Long hiredCount;
}
