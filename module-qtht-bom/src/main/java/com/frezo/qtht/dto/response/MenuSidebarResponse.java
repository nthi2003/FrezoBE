package com.frezo.qtht.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight DTO for Sidebar Menu (only essential fields)
 * Reduces payload size by ~60% compared to full MenuResponse
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuSidebarResponse {
    private String id;
    private String code;
    private String name;
    private String parentCode;
    private String feUrl;
    private String icon;
    private Integer orderIndex;
}
