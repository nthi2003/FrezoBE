package com.frezo.email.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EmailTemplateRequest {
    private String name;

    private String code;

    private String subject;

    private String content;

    private String description;
}
