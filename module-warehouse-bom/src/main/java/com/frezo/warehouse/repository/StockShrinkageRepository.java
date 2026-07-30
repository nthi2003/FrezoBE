package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.StockShrinkage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockShrinkageRepository extends JpaRepository<StockShrinkage, String> {

    Optional<StockShrinkage> findByShrinkageCodeAndIsDeletedFalse(String shrinkageCode);

    List<StockShrinkage> findByIsDeletedFalseOrderByCreatedDateDesc();

    List<StockShrinkage> findByWarehouseIdAndIsDeletedFalseOrderByCreatedDateDesc(String warehouseId);

    List<StockShrinkage> findByStatusAndIsDeletedFalseOrderByCreatedDateDesc(String status);
}
