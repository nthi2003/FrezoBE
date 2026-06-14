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
    private Integer statusCode;
    private String search;
    private String ipAddress;
    private String username;
    private String uri;
    private Long durationMin;
    private Long durationMax;
}
