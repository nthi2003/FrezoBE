package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.PurchaseOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, String> {

    List<PurchaseOrderLine> findByPurchaseOrderIdAndIsDeletedFalse(String purchaseOrderId);
}
