package com.frezo.qtht.entity;

import com.frezo.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiLog extends BaseEntity {

    private String uri; // /api/contract/add
    private String method; // GET, POST...
    private String ipAddress; // IP người gọi
    private String username; // user đang đăng nhập
    private Integer statusCode; // 200, 400, 500
    private Long duration; // ms
    private LocalDateTime effTo;
    private LocalDateTime effFrom;
    @Column(columnDefinition = "text")
    private String requestBody;

    @Column(columnDefinition = "text")
    private String responseBody;
}
