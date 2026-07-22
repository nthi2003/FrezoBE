package com.frezo.event.dto.request;

import lombok.Data;

@Data
public class EventSaveRequest {
    private String title;
    private String description;
    private String location;
    /** ISO-8601 local datetime */
    private String startAt;
    private String endAt;
    private Integer capacity;
    private String coverUrl;
}
