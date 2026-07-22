package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.StockTakeLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockTakeLineRepository extends JpaRepository<StockTakeLine, String> {
    List<StockTakeLine> findByStockTakeIdAndIsDeletedFalse(String stockTakeId);
}
