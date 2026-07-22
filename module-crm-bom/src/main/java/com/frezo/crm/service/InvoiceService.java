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
    /** Ghi nhận đã thanh toán một phần / toàn bộ. Tự update status. */
    Invoice recordPayment(String id, BigDecimal amount, String paymentAccountCode);

    /** Tạo bút toán bán hàng: Nợ 131 | Có 511 + 3331. Idempotent. */
    Invoice postToGL(String id);
}
