package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.StockLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockLedgerRepository extends JpaRepository<StockLedger, String> {
    List<StockLedger> findByProductId(String productId);
    List<StockLedger> findByTransactionId(String transactionId);
    List<StockLedger> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);
}
