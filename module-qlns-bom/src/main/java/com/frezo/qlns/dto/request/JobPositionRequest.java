package com.frezo.qlns.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JobPositionRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String rankCode;
    @NotBlank
    private String titleCode;
    private Boolean activated = true;
    private Integer orderIndex;
}
