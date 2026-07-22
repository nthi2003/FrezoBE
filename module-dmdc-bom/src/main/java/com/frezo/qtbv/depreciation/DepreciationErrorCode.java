package com.frezo.qtbv.depreciation;

import com.frezo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DepreciationErrorCode implements ErrorCode {

    ASSET_NOT_FOUND("depreciation.asset.not.found", HttpStatus.NOT_FOUND,
            "Tài sản không tồn tại"),
    ASSET_MISSING_PRICE("depreciation.asset.missing.price", HttpStatus.BAD_REQUEST,
            "Tài sản thiếu giá mua để tính khấu hao"),
    SCHEDULE_EXISTS("depreciation.schedule.exists", HttpStatus.CONFLICT,
            "Đã có lịch khấu hao cho tài sản này"),
    METHOD_INVALID("depreciation.method.invalid", HttpStatus.BAD_REQUEST,
            "Phương pháp khấu hao không hợp lệ"),
    NO_ACTIVE_SCHEDULE("depreciation.no.active.schedule", HttpStatus.BAD_REQUEST,
            "Không có lịch khấu hao đang hiệu lực để ghi sổ trong kỳ này");

    private final String key;
    private final HttpStatus status;
    private final String defaultMessage;
}
