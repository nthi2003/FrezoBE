package com.frezo.email.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailInboxResponse {
    private String messageId;
    private String subject;
    private String from;
    private String fromPersonal;
    private List<String> to;
    private LocalDateTime sentDate;
    private String bodyPreview;
    private String bodyHtml;
    private boolean seen;
    private boolean hasAttachments;
    private List<String> attachmentNames;
}
