package com.frezo.qlns.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobPositionResponse {
    private String id;
    private String name;
    private String rankCode;
    private String rankName;
    private String titleCode;
    private String titleName;
    private Boolean activated;
    private Integer orderIndex;
}
