package com.frezo.email.dto.request;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SendTestEmailRequest {
    private List<String> recipients;
    private Map<String, Object> params;
}
