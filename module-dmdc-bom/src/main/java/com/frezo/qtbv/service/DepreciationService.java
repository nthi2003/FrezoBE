package com.frezo.qtbv.service;

import com.frezo.qtbv.dto.request.DepreciationScheduleRequest;
import com.frezo.qtbv.dto.response.DepreciationPostingResponse;
import com.frezo.qtbv.dto.response.DepreciationScheduleResponse;

import java.util.List;

public interface DepreciationService {


    DepreciationScheduleResponse generateSchedule(DepreciationScheduleRequest req);


    List<DepreciationScheduleResponse> listSchedules(String assetId);


    DepreciationPostingResponse postPeriod(int year, int month);


    DepreciationPostingResponse previewPeriod(int year, int month);


    List<DepreciationPostingResponse> listPostings(Integer year, Integer month);
}
