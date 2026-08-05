package com.frezo.dmdc.service;

import com.frezo.dmdc.dto.response.UxPopupResponse;

import java.util.Optional;

public interface UxPopupService {
    /**
     * Resolve template theo event code (category.code trong group UX_POPUP).
     * Empty khi không tìm thấy hoặc inactive.
     */
    Optional<UxPopupResponse> resolve(String eventCode);
}
