package com.frezo.qlns.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class OkrCheckInRequest {
    private String note;
    private List<OkrKeyResultRequest> keyResults;
}
