package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.StockAdjustmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockAdjustmentItemRepository extends JpaRepository<StockAdjustmentItem, String> {
    List<StockAdjustmentItem> findByAdjustmentId(String adjustmentId);
    void deleteByAdjustmentId(String adjustmentId);
}
