package com.frezo.qlns.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ResignationApproveRequest {

    private LocalDate actualLastDay;
    private String note;
}
