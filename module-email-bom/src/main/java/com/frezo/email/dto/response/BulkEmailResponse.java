package com.frezo.email.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkEmailResponse {
    private long totalRecipients;
    private long sentSuccess;
    private long sentFailed;
    private List<String> failedEmails;
}
