package com.frezo.qtht.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GuideSaveRequest {

    @NotBlank
    @Size(max = 120)
    private String slug;

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String body;

    @Size(max = 100)
    private String module;

    @Size(max = 500)
    private String summary;

    private Integer sortOrder;

    private Boolean published;
}
