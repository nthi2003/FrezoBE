package com.frezo.auth.service.impl;

import com.frezo.auth.repository.LoginHistoryRepository;
import com.frezo.auth.service.UserActivityService;
import com.frezo.auth.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserActivityServiceImpl implements UserActivityService {

    private static final int LOGIN_CHART_DAYS = 30;

    private final LoginHistoryRepository loginHistoryRepository;
    private final UserSessionService userSessionService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> loginByDayLast30() {
        LocalDateTime from = LocalDate.now().minusDays(LOGIN_CHART_DAYS).atStartOfDay();
        Map<String, Long> history = loginHistoryRepository
                .findByStatusAndLoginTimeGreaterThanEqual("SUCCESS", from)
                .stream()
                .collect(Collectors.groupingBy(
                        h -> h.getLoginTime().toLocalDate().toString(),
                        Collectors.counting()));

        Map<String, Long> sorted = new LinkedHashMap<>();
        for (int i = LOGIN_CHART_DAYS - 1; i >= 0; i--) {
            String key = LocalDate.now().minusDays(i).toString();
            sorted.put(key, history.getOrDefault(key, 0L));
        }
        return sorted;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> usageSummary(int onlineSeconds) {
        int window = Math.max(30, Math.min(onlineSeconds <= 0 ? 90 : onlineSeconds, 3600));
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("date", today.toString());
        data.put("loginsToday", loginHistoryRepository.countSuccessBetween(start, end));
        data.put("uniqueUsersToday", loginHistoryRepository.countDistinctUsersSuccessBetween(start, end));
        data.put("onlineUsers", userSessionService.countOnlineUsers(window));
        data.put("activeSessions", userSessionService.countAllActiveSessions());
        data.put("onlineWindowSeconds", window);
        data.put("onlineWindowMinutes", Math.max(1, (window + 59) / 60));
        data.put("asOf", LocalDateTime.now().toString());
        return data;
    }
}
