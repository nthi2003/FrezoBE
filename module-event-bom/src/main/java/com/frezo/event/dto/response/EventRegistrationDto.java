package com.frezo.event.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRegistrationDto {
    private String id;
    private String eventId;
    private String eventTitle;
    private String username;
    private String displayName;
    private String email;
    private String rsvpStatus;
    private String note;
    private String registeredAt;
}
