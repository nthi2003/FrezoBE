package com.frezo.qlns.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class RegularizationAddRequest {
    private String personId;
    private String contractId;
    private LocalDate attendanceDate;
    private LocalTime requestedCheckIn;
    private LocalTime requestedCheckOut;
    private String reason;
    /** Optional — nếu FE biết trước manager. Không có thì service tự resolve theo department. */
    private String managerUsername;
}
