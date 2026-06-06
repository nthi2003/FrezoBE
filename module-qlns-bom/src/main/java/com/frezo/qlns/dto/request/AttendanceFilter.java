package com.frezo.qlns.dto.request;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import lombok.Data;

@Data
public class AttendanceFilter {
    private String contractId;
    private String personId;
    private Integer month;
    private Integer year;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
    private String status;
    private Integer pageNumber;
    private Integer pageSize = 10;
}
