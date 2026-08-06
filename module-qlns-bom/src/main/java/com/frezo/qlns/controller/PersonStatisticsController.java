package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.qlns.dto.response.PersonStatisticsResponse;
import com.frezo.qlns.service.PersonStatisticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/qlns/person-statistics")
@RequiredArgsConstructor
@Tag(name = "HR - Thống kê nhân sự")
public class PersonStatisticsController {

    private final PersonStatisticsService personStatisticsService;

    @GetMapping
    @CheckPermission(api = "/qlns/person-statistics", action = "VIEW")
    public ApiResponse<PersonStatisticsResponse> getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(personStatisticsService.getStatistics(from, to));
    }
}
