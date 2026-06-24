package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.StockTransferItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransferItemRepository extends JpaRepository<StockTransferItem, String> {
    List<StockTransferItem> findByTransferId(String transferId);
}
