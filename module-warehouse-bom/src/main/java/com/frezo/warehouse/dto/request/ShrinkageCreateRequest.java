package com.frezo.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class ShrinkageCreateRequest {

    @NotBlank
    private String warehouseId;

    private String note;

    @NotNull
    private List<ShrinkageLineRequest> lines;

    @Data
    public static class ShrinkageLineRequest {
        @NotBlank
        private String batchId;
        @NotBlank
        private String productId;
        /** SHRINK / DAMAGE / EXPIRED */
        @NotBlank
        private String reason;
        @NotNull
        @Positive
        private Double qty;
        private String note;
    }
}
