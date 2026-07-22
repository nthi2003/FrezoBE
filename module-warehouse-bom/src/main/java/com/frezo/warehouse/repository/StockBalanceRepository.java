package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.StockBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockBalanceRepository extends JpaRepository<StockBalance, String>, JpaSpecificationExecutor<StockBalance> {
    Optional<StockBalance> findByProductIdAndWarehouseIdAndLocationIdAndBatchId(
            String productId, String warehouseId, String locationId, String batchId);

    @Query(value = """
            SELECT sb.id FROM stock_balances sb
            JOIN products p ON p.id = sb.product_id
            WHERE p.warning_threshold IS NOT NULL
            AND sb.quantity_available <= p.warning_threshold""", nativeQuery = true)
    List<String> findLowStockBalanceIds();

    @Query("SELECT COUNT(DISTINCT s.productId) FROM StockBalance s")
    long countDistinctProductId();

    @Query("""
            SELECT COALESCE(SUM(s.quantityAvailable), 0) FROM StockBalance s
            WHERE s.productId = :productId AND s.warehouseId = :warehouseId
              AND (s.isDeleted = false OR s.isDeleted IS NULL)
            """)
    Double sumAvailableByProductAndWarehouse(
            @org.springframework.data.repository.query.Param("productId") String productId,
            @org.springframework.data.repository.query.Param("warehouseId") String warehouseId);
}
