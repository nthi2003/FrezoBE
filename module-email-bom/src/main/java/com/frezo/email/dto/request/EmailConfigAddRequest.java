package com.frezo.email.dto.request;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailConfigAddRequest {
    public String code;
    @Column(nullable = false, unique = true, length = 100)
    public String name;
    @Column(name = "api_key", length = 100, nullable = false)
    public String apiKey;
    @Column(nullable = false, unique = true, length = 100)
    private String smtp;

    private String orgId;

    private Short port;
    @Column(nullable = false, unique = true, length = 100)
    private String nameEmail;
}
