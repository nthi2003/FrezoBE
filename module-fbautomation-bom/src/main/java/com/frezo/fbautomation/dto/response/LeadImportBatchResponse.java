package com.frezo.fbautomation.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class LeadImportBatchResponse {
    private String id;
    private String filename;
    private String source;
    private Integer rowCount;
    private Integer successCount;
    private Integer skippedCount;
    private Integer failedCount;
    private String errorLog;
    private String uploadedBy;
    private OffsetDateTime uploadedAt;
    private Boolean rolledBack;
}
