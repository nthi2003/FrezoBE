package com.frezo.fbautomation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationSummaryResponse {
    private long totalAccounts;
    private long activeAccounts;
    private long totalGroups;
    private long approvedGroups;
    private long totalLeads;
    private long importedLeads;
    private long pendingLeads;
}
