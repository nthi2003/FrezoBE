package com.frezo.qlns.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ResignationCreateRequest {

    private String personId;
    private LocalDate expectedLastDay;
    private String reason;
}
