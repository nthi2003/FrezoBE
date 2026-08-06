package com.frezo.email.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/** Một lần gửi email đã ghi nhận (kể cả thất bại) — hiển thị cho admin. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailLogResponse {

    private String id;
    private String topic;
    private List<String> recipients;
    /** Kênh gửi — EMAIL. */
    private String type;
    /** SUCCESS | FAILED. */
    private String status;
    private String errorMessage;
    private String description;
    private String emailTemplateId;
    private LocalDateTime createdDate;
    private String createdBy;
}
