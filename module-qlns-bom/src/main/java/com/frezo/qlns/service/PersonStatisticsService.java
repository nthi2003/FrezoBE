package com.frezo.qlns.service;

import com.frezo.qlns.dto.response.PersonStatisticsResponse;

import java.time.LocalDate;

public interface PersonStatisticsService {
    PersonStatisticsResponse getStatistics(LocalDate from, LocalDate to);
}
