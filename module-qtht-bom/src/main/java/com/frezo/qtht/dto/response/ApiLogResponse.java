package com.frezo.qtht.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApiLogResponse {
    private String id;
    private String uri;
    private String method;
    private String ipAddress;
    private String username;
    private Integer statusCode;
    private Long duration;
    private LocalDateTime effTo;
    private LocalDateTime effFrom;
    private String requestBody;
    private String responseBody;
    private LocalDateTime createdDate;
    private String userAgent;
    private String queryString;
    private String module;
    private String errorMessage;
    private String traceId;
}
