package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, String> {
    Optional<StockTransaction> findByTransactionCode(String transactionCode);
    List<StockTransaction> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);
}
