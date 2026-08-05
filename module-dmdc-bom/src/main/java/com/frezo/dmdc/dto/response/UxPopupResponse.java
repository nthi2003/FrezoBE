package com.frezo.dmdc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Template popup UX resolve từ category group {@code UX_POPUP}.
 * <ul>
 *   <li>{@code eventCode} = category.code</li>
 *   <li>{@code title} = category.name</li>
 *   <li>{@code body} = description plain hoặc JSON {@code {"body","imageUrl"}}</li>
 *   <li>{@code enabled} = category.active</li>
 * </ul>
 */
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
