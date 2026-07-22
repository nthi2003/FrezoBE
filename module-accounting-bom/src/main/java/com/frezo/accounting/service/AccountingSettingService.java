package com.frezo.accounting.service;

import com.frezo.accounting.dto.request.AccountingSettingRequest;
import com.frezo.accounting.dto.response.AccountingSettingResponse;
import com.frezo.accounting.entity.AccountingSetting;

/**
 * Cấu hình kế toán (singleton). Đọc/ghi record duy nhất.
 */
public interface AccountingSettingService {

    /** Lấy setting hiện tại. Nếu chưa có, tạo default (TT133 + mapping mặc định). */
    AccountingSetting getOrCreateDefault();

    AccountingSettingResponse view();

    /** Update setting + optionally seed COA. */
    AccountingSettingResponse update(AccountingSettingRequest req);
}
