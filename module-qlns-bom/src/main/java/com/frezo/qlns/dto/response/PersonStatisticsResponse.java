package com.frezo.qlns.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class PersonStatisticsResponse {
    private long total;
    private long male;
    private long female;
    private long newHires;
    private long official;
    private long resigned;
    private List<Map<String, Object>> contractExpiring;
    private List<Map<String, Object>> probation;
    private List<Map<String, Object>> officialList;
    private List<Map<String, Object>> resignedList;
    private List<Map<String, Object>> birthdays;
}
