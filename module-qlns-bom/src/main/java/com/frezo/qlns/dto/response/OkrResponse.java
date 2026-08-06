package com.frezo.qlns.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OkrResponse {
    private String id;
    private String title;
    private String description;
    private String ownerPersonId;
    private String periodLabel;
    private String cycleId;
    private String departmentId;
    private String orgId;
    private String parentOkrId;
    private String scopeType;
    private String objectiveType;
    private List<String> crossLinkIds;
    private Boolean published;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Double progress;
    /** Alias FE — cùng giá trị {@link #progress}. */
    private Double progressPct;
    private List<OkrKeyResultResponse> keyResults;
}
