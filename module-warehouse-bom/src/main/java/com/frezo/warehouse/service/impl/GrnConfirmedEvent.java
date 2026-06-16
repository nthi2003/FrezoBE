package com.frezo.warehouse.service.impl;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class GrnConfirmedEvent extends ApplicationEvent {
    private final String grnId;
    private final String grnCode;
    private final String purchaseOrderId;
    private final Double totalValue;

    public GrnConfirmedEvent(Object source, String grnId, String grnCode,
                             String purchaseOrderId, Double totalValue) {
        super(source);
        this.grnId = grnId;
        this.grnCode = grnCode;
        this.purchaseOrderId = purchaseOrderId;
        this.totalValue = totalValue;
    }
}
