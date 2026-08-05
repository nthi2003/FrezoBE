package com.frezo.qtht.service;

import com.frezo.common.response.FePage;
import com.frezo.qtht.dto.request.SystemJobUpdateRequest;
import com.frezo.qtht.dto.response.SystemJobDto;
import com.frezo.qtht.dto.response.SystemJobHistoryDto;

import java.time.LocalDateTime;
import java.util.List;

/** Quản trị tác vụ nền: xem, đổi cron, bật tắt, chạy tay, xem lịch sử. */
public interface SystemJobService {

    List<SystemJobDto> listJobs();

    SystemJobDto updateJob(String code, SystemJobUpdateRequest request);

    /** Kích hoạt job ngay (async) — trả về trạng thái job sau khi nhận lệnh. */
    SystemJobDto runNow(String code);

    FePage<SystemJobHistoryDto> history(String code, Integer pageNumber, Integer pageSize,
                                        String status, LocalDateTime fromDate, LocalDateTime toDate);

    /** Các mốc chạy kế tiếp của một biểu thức cron (ISO datetime) — dùng để preview trước khi lưu. */
    List<String> previewCron(String expression, Integer count);
}
