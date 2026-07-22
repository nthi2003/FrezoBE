package com.frezo.event.dto.request;

import lombok.Data;

@Data
public class RsvpRequest {
    /** GOING / MAYBE / DECLINED */
    private String status;
    private String note;
    private String displayName;
    private String email;
}
