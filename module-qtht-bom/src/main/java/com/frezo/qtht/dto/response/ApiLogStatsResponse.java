package com.frezo.qtht.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ApiLogStatsResponse {
    private long total;
    private long success;
    private long failed;
    private double avgDuration;
    private double totalTrend;  // % so với kỳ trước
    private double failedTrend; // % so với kỳ trước
}
