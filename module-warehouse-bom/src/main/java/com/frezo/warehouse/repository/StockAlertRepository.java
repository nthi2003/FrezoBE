package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockAlertRepository extends JpaRepository<StockAlert, String> {

    List<StockAlert> findByIsDeletedFalseOrderByTriggeredAtDesc();

    List<StockAlert> findByStatusAndIsDeletedFalseOrderByTriggeredAtDesc(String status);

    Optional<StockAlert> findByIdempotencyKeyAndIsDeletedFalse(String key);

    Optional<StockAlert> findByProductIdAndWarehouseIdAndStatusAndIsDeletedFalse(
            String productId, String warehouseId, String status);
}
