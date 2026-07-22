package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, String> {

    List<PurchaseOrder> findByIsDeletedFalseOrderByCreatedDateDesc();

    Optional<PurchaseOrder> findByPrIdAndIsDeletedFalse(String prId);
}
