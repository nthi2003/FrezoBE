package com.frezo.qlns.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class OkrRequest {
    private String title;
    private String description;
    private String ownerPersonId;
    private String periodLabel;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private List<OkrKeyResultRequest> keyResults;
}
