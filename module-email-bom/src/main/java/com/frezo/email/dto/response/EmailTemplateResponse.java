package com.frezo.email.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplateResponse {

    private String name;

    private String subject;

    private String content;

    private String description;
}
