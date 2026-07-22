package com.frezo.event.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {
    private String id;
    private String title;
    private String description;
    private String location;
    private String startAt;
    private String endAt;
    private String status;
    private Integer capacity;
    private Integer registeredCount;
    private Integer seatsLeft;
    private String coverUrl;
    private String organizerUsername;
    private String publishedAt;
    private String cancelledAt;
    private String createdDate;
    /** RSVP của user hiện tại (portal) — null nếu chưa đăng ký. */
    private String myRsvpStatus;
}
