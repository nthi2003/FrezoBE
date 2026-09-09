package com.frezo.dmdc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UxPopupResponse {
    private String eventCode;
    private String title;
    private String body;
    private String imageUrl;
    private boolean enabled;
}
