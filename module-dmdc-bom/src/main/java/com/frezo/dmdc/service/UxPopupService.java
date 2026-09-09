package com.frezo.dmdc.service;

import com.frezo.dmdc.dto.response.UxPopupResponse;

import java.util.Optional;

public interface UxPopupService {

    Optional<UxPopupResponse> resolve(String eventCode);
}
