package com.frezo.qlns.service;

import com.frezo.qlns.dto.response.TimesheetReportResponse;

import java.util.List;

public interface TimesheetReportService {
    List<TimesheetReportResponse> getTimesheet(int month, int year, String personId);
    byte[] exportTimesheetExcel(int month, int year, String personId);
}
