package com.frezo.fbautomation.service;

import com.frezo.fbautomation.dto.response.AutomationSummaryResponse;
import com.frezo.fbautomation.dto.response.FacebookGroupResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface FacebookAutomationService {

    CompletableFuture<List<FacebookGroupResponse>> searchAndScrapeGroups(String accountId, String keyword, Integer maxResults);

    CompletableFuture<String> autoJoinGroup(String accountId, String groupId);

    void loginWithCookie(String accountId);

    AutomationSummaryResponse getSummary();
}
