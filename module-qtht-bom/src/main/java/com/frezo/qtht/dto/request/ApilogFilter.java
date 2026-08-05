package com.frezo.qtht.dto.request;

import com.frezo.common.model.PagingBase;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ApilogFilter extends PagingBase {
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private String method;
    /** Exact status code (legacy). Prefer {@link #statusGroup}. */
    private Integer statusCode;
    /**
     * Nhóm status: {@code 2xx}, {@code 3xx}, {@code 4xx}, {@code 5xx}, hoặc {@code all}.
     */
    private String statusGroup;
    /** Chỉ lấy status >= 400. */
    private Boolean errorsOnly;
    private String search;
    private String ipAddress;
    private String username;
    private String uri;
    /** Module/service (segment đầu path). */
    private String module;
    private Long durationMin;
    private Long durationMax;
}
