package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.StockBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockBalanceRepository extends JpaRepository<StockBalance, String> {
    Optional<StockBalance> findByProductIdAndWarehouseIdAndLocationIdAndBatchId(
            String productId, String warehouseId, String locationId, String batchId);
}
