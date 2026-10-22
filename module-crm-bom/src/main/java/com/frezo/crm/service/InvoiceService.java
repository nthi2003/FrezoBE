package com.frezo.crm.service;

import com.frezo.crm.dto.InvoiceRequest;
import com.frezo.crm.entity.Invoice;
import com.frezo.crm.entity.InvoiceItem;

import java.math.BigDecimal;
import java.util.List;

public interface InvoiceService {
    Invoice create(InvoiceRequest req);
    Invoice update(String id, InvoiceRequest req);
    void delete(String id);
    Invoice get(String id);
    List<Invoice> list();
    List<InvoiceItem> items(String invoiceId);
    Invoice issue(String id);
    Invoice recordPayment(String id, BigDecimal amount, String paymentAccountCode);
    Invoice postToGL(String id);
}
