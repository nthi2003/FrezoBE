package com.frezo.warehouse.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class FromAlertsRequest {
    private List<String> alertIds;
}
