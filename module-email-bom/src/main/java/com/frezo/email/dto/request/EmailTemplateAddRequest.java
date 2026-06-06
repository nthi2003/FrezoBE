package com.frezo.email.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplateAddRequest {
    private String name;

    private String code;

    private String subject;

    private String description;
}
