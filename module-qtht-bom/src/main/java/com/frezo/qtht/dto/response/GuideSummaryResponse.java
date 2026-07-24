package com.frezo.qtht.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuideSummaryResponse {

    private String id;
    private String slug;
    private String title;
    private String module;
    private String summary;
    private Integer sortOrder;
    private Boolean published;
    private String updatedBy;
    private LocalDateTime updatedDate;
}
