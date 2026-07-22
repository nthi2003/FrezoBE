package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.ReorderRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReorderRuleRepository extends JpaRepository<ReorderRule, String> {

    List<ReorderRule> findByIsDeletedFalseOrderByUpdatedDateDesc();

    List<ReorderRule> findByWarehouseIdAndIsDeletedFalse(String warehouseId);

    List<ReorderRule> findByActiveTrueAndIsDeletedFalse();

    Optional<ReorderRule> findByProductIdAndWarehouseIdAndIsDeletedFalse(String productId, String warehouseId);
}
