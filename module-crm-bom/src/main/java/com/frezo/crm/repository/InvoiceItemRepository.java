package com.frezo.crm.repository;

import com.frezo.crm.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, String> {
    List<InvoiceItem> findByInvoiceIdOrderByLineNoAsc(String invoiceId);
    void deleteByInvoiceId(String invoiceId);
}
