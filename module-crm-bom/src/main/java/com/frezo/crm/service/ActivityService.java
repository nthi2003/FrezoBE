package com.frezo.crm.service;

import com.frezo.crm.dto.ActivityRequest;
import com.frezo.crm.entity.DealActivity;

import java.util.List;

public interface ActivityService {
    DealActivity log(ActivityRequest req);
    void delete(String id);
    List<DealActivity> byDeal(String dealId);
    List<DealActivity> byCustomer(String customerId);
}
