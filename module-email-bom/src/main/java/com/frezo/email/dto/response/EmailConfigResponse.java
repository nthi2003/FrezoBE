package com.frezo.email.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailConfigResponse {
    private String id;
    private String code;
    private String name;
    private String apiKey;
    private String smtp;
    private Short port;
    private String nameEmail;
}
