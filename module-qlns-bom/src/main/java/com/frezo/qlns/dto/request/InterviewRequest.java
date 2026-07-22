package com.frezo.qlns.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InterviewRequest {

    private String applicationId;
    /** PHONE / ONLINE / ONSITE / TECHNICAL / HR / FINAL. */
    private String type;
    private LocalDateTime scheduledAt;
    private String interviewerUsername;
    private String location;
    private String meetingLink;
}
