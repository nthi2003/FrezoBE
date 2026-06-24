package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.StockLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockLedgerRepository extends JpaRepository<StockLedger, String> {
    List<StockLedger> findByProductId(String productId);
    List<StockLedger> findByTransactionId(String transactionId);
    List<StockLedger> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);

    @Query("""
            SELECT s FROM StockLedger s
            WHERE (:from IS NULL OR s.createdDate >= :from)
            AND (:to IS NULL OR s.createdDate <= :to)
            AND (:productId IS NULL OR s.productId = :productId)
            AND (:warehouseId IS NULL OR s.warehouseId = :warehouseId)
            AND (:keyword IS NULL OR s.note LIKE %:keyword% OR s.referenceId LIKE %:keyword%)
            ORDER BY s.createdDate ASC""")
    List<StockLedger> searchMovements(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("productId") String productId,
            @Param("warehouseId") String warehouseId,
            @Param("keyword") String keyword);
}
