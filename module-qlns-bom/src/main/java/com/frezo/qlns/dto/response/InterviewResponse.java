package com.frezo.qlns.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewResponse {

    private String id;
    private String applicationId;
    private String type;
    private LocalDateTime scheduledAt;
    private String interviewerUsername;
    private String location;
    private String meetingLink;
    private String status;
    private Double score;
    private String feedback;
}
