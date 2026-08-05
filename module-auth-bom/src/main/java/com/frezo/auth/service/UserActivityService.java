package com.frezo.auth.service;

import java.util.Map;

public interface UserActivityService {

    /** Login SUCCESS theo ngày — đủ 30 ngày (0 nếu trống) để FE chart ổn định. */
    Map<String, Long> loginByDayLast30();

    /** Tóm tắt usage hôm nay cho hub /qtht/usage. onlineSeconds = cửa sổ coi là online. */
    Map<String, Object> usageSummary(int onlineSeconds);
}
