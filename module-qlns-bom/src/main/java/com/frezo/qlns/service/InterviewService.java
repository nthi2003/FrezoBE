package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.InterviewCompleteRequest;
import com.frezo.qlns.dto.request.InterviewRequest;
import com.frezo.qlns.dto.response.InterviewResponse;

import java.util.List;

public interface InterviewService {

    InterviewResponse create(InterviewRequest req);

    List<InterviewResponse> list(String applicationId);

    InterviewResponse complete(String id, InterviewCompleteRequest req);
}
