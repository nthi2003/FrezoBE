package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, String>, JpaSpecificationExecutor<StockAdjustment> {
    Optional<StockAdjustment> findByAdjustmentCode(String adjustmentCode);
}
