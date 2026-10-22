package com.frezo.email.dto.request;

import com.frezo.common.model.PagingBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Filter lịch sử gửi email — dùng cho {@code GET /email/send/log}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailLogFilter extends PagingBase {
    private String keyword;
    private String status;
    private String type;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
}
