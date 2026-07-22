package com.frezo.warehouse.service;

import com.frezo.common.response.FePage;
import com.frezo.warehouse.dto.request.ReorderRuleRequest;
import com.frezo.warehouse.dto.response.ReorderRuleDto;
import com.frezo.warehouse.dto.response.StockAlertDto;
import com.frezo.warehouse.dto.response.WarehouseOptionDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ReorderService {

    List<WarehouseOptionDto> listWarehouses();

    FePage<ReorderRuleDto> listRules(String warehouseId, String productId);

    ReorderRuleDto create(ReorderRuleRequest req);

    ReorderRuleDto update(String id, ReorderRuleRequest req);

    void delete(String id);

    Map<String, Integer> importExcel(MultipartFile file);

    FePage<StockAlertDto> listAlerts(String status);

    StockAlertDto dismiss(String id);

    /** Cron job entry — scan active rules. */
    void scanAndRaiseAlerts();
}
