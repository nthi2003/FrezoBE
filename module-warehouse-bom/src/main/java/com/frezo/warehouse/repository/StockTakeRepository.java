package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.StockTake;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockTakeRepository extends JpaRepository<StockTake, String> {
    List<StockTake> findByIsDeletedFalseOrderByCreatedDateDesc();
    List<StockTake> findByWarehouseIdAndIsDeletedFalse(String warehouseId);
}
