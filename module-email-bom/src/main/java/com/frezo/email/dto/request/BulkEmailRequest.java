package com.frezo.email.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkEmailRequest {
    private String templateCode;
    private String subject;
    private String body;
    private List<String> recipients;
    private List<String> categoryCodes;
    private String description;
}
