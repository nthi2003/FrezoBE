package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.StockShrinkageLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockShrinkageLineRepository extends JpaRepository<StockShrinkageLine, String> {

    List<StockShrinkageLine> findByShrinkageIdAndIsDeletedFalse(String shrinkageId);
}
