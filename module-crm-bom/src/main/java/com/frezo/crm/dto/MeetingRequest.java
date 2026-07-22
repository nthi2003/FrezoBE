package com.frezo.crm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MeetingRequest {
    private String title;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String dealId;
    private String customerId;
    private String location;
    private String meetingLink;
    private String attendees;
    private String status;
    private String notes;
}
