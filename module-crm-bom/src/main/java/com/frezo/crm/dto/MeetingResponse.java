package com.frezo.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingResponse {
    private String id;
    private String title;
    private String startAt;
    private String endAt;
    private String dealId;
    private String customerId;
    private String location;
    private String meetingLink;
    private String attendees;
    private String status;
    private String notes;
}
