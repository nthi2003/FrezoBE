package com.frezo.qtht.entity;

import com.frezo.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_log")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiLog extends BaseEntity {

    private String uri; // /qtht/users (không gồm context-path)
    private String method; // GET, POST...
    private String ipAddress; // IP người gọi
    private String username; // user đang đăng nhập (hoặc ANONYMOUS)
    private Integer statusCode; // 200, 400, 500
    private Long duration; // ms
    private LocalDateTime effTo;
    private LocalDateTime effFrom;

    @Column(columnDefinition = "text")
    private String requestBody;

    @Column(columnDefinition = "text")
    private String responseBody;

    /** User-Agent header (rút gọn). */
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /** Query string (không gồm '?'). */
    @Column(name = "query_string", length = 2000)
    private String queryString;

    /** Module/service suy ra từ segment đầu path (qtht, qlns, auth…). */
    @Column(name = "module", length = 64)
    private String module;

    /** Message lỗi rút gọn khi status >= 400. */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /** Trace / correlation id nếu có (X-Request-Id, X-Trace-Id…). */
    @Column(name = "trace_id", length = 64)
    private String traceId;
}
