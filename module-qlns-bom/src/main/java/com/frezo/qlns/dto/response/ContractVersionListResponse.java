package com.frezo.qlns.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ContractVersionListResponse {
    private String id;
    private Integer versionNumber;
    private String updateType;
    private String description;
    private String createdBy;
    private LocalDateTime createdDate;
    private String snapshotJson;
}
