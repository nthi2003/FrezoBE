package com.frezo.qtbv.dto.request;

import com.frezo.common.model.PagingBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CategoryFilter extends PagingBase {
    private String keyword;
    private Boolean active;
    /** Alias của {@link #groupCode} — giữ tương thích FE cũ. */
    @Schema(description = "Alias của groupCode (cùng cột group_code). VD: ChucDanh")
    private String type;
    /** Canonical filter. Chức danh = ChucDanh (legacy TITLE đã migrate). */
    @Schema(description = "Canonical group code. Chức danh: ChucDanh", example = "ChucDanh")
    private String groupCode;
}
