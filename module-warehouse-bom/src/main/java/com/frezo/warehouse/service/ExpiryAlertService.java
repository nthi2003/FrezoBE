package com.frezo.warehouse.service;

public interface ExpiryAlertService {

    /** Quét lô cận hạn → tạo StockAlert type EXPIRY_SOON. */
    int scanAndRaiseExpiryAlerts();
}
