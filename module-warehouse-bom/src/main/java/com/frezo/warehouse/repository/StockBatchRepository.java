package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.StockBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockBatchRepository extends JpaRepository<StockBatch, String> {

    Optional<StockBatch> findByBatchCodeAndIsDeletedFalse(String batchCode);

    List<StockBatch> findByWarehouseIdAndProductIdAndIsDeletedFalseOrderByExpiryDateAsc(
            String warehouseId, String productId);

    @Query("""
            SELECT b FROM StockBatch b
            WHERE b.isDeleted = false AND b.qtyOnHand > 0
              AND b.warehouseId = :warehouseId AND b.productId = :productId
              AND b.status = 'ACTIVE'
            ORDER BY b.expiryDate ASC NULLS LAST, b.receivedDate ASC
            """)
    List<StockBatch> findAvailableForFefo(
            @Param("warehouseId") String warehouseId,
            @Param("productId") String productId);

    List<StockBatch> findByWarehouseIdAndIsDeletedFalseOrderByExpiryDateAsc(String warehouseId);

    List<StockBatch> findByWarehouseIdAndProductIdAndIsDeletedFalse(String warehouseId, String productId);

    @Query("""
            SELECT b FROM StockBatch b
            WHERE b.isDeleted = false AND b.qtyOnHand > 0
              AND b.status = 'ACTIVE'
              AND b.expiryDate IS NOT NULL
              AND b.expiryDate <= :threshold
            """)
    List<StockBatch> findExpiringBefore(@Param("threshold") LocalDate threshold);

    long countByGrnIdAndProductIdAndIsDeletedFalse(String grnId, String productId);

    @Query("""
            SELECT COUNT(b) FROM StockBatch b
            WHERE b.isDeleted = false AND b.batchCode LIKE CONCAT(:prefix, '%')
            """)
    long countByBatchCodePrefix(@Param("prefix") String prefix);
}
