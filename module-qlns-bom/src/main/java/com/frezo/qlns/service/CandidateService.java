package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.CandidateRequest;
import com.frezo.qlns.dto.response.CandidateResponse;

import java.util.List;

public interface CandidateService {

    CandidateResponse create(CandidateRequest req);

    CandidateResponse getById(String id);

    List<CandidateResponse> search(String keyword);
}
