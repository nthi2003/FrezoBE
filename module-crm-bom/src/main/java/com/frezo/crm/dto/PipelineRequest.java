package com.frezo.crm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class PipelineRequest {
    @NotBlank
    private String name;
    private String description;
    private Boolean isDefault;
    private Boolean active;

    /** Nếu truyền, tạo/cập nhật stages. */
    private List<StageInline> stages;

    @Data
    public static class StageInline {
        private String id;
        @NotBlank
        private String name;
        private Integer orderNo;
        private Integer probability;
        private Boolean won;
    }
}
