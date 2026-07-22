package com.frezo.qlns.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Filter danh sách bảng lương. pageNumber mặc định 1 (1-based) khi omit.")
public class PayrollFilter {
    private String contractId;
    private String personId;
    private Integer month;
    private Integer year;
    private Integer status; // 0=DRAFT, 1=CONFIRMED, 2=PAID

    @Schema(description = "Trang (1-based). Omit → BE default 1 — không NPE.", example = "1", defaultValue = "1")
    private Integer pageNumber = 1;

    @Schema(description = "Số bản ghi / trang. Màn kỳ có thể dùng tới 500.", example = "10", defaultValue = "10")
    private Integer pageSize = 10;
}
