package com.frezo.fbautomation.service;

import com.frezo.fbautomation.dto.request.FacebookAccountRequest;
import com.frezo.fbautomation.dto.response.AutomationSummaryResponse;
import com.frezo.fbautomation.dto.response.FacebookAccountResponse;

import java.util.List;
import java.util.Map;

public interface FacebookAccountService {

    List<FacebookAccountResponse> getAll();

    FacebookAccountResponse getById(String id);

    FacebookAccountResponse create(FacebookAccountRequest request);

    FacebookAccountResponse update(String id, FacebookAccountRequest request);

    void delete(String id);

    void updateCookie(String id, String cookie);

    void incrementPostsToday(String id);

    void resetDailyPostCount();
}
