package com.frezo.qlns.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private long pendingTasks;
    private long todayAttendance;
    private long newArticles;
    private List<ContractExpiryDTO> expiringContracts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractExpiryDTO {
        private String contractCode;
        private String employeeName;
        private String expiryDate;
        private long daysRemaining;
    }
}
