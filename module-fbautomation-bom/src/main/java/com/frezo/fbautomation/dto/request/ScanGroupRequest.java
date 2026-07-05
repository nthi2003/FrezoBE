package com.frezo.fbautomation.dto.request;

import lombok.Data;

@Data
public class ScanGroupRequest {
    private String keyword;
    private String accountId;
    private Integer maxResults;
}
