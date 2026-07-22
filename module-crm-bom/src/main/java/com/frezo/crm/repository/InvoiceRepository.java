package com.frezo.crm.repository;

import com.frezo.crm.common.InvoiceStatus;
import com.frezo.crm.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {
    Optional<Invoice> findByCode(String code);
    List<Invoice> findByCustomerIdAndIsDeletedFalseOrderByIssuedDateDesc(String customerId);
    List<Invoice> findByStatusAndIsDeletedFalseOrderByIssuedDateDesc(InvoiceStatus status);
    List<Invoice> findByIsDeletedFalseOrderByIssuedDateDesc();
    long countByIsDeletedFalse();
}
