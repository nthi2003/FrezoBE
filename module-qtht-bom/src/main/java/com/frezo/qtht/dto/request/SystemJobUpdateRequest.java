package com.frezo.qtht.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Cập nhật cron và/hoặc bật tắt job — ít nhất một field phải có giá trị. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemJobUpdateRequest {

    private String cronExpression;

    private Boolean enabled;
}
