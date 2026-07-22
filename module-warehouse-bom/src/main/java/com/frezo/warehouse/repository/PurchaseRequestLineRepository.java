package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.PurchaseRequestLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRequestLineRepository extends JpaRepository<PurchaseRequestLine, String> {

    List<PurchaseRequestLine> findByPurchaseRequestIdAndIsDeletedFalse(String purchaseRequestId);

    boolean existsByStockAlertIdAndIsDeletedFalse(String stockAlertId);
}
