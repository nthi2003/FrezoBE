package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.GoodsReceiptNoteItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoodsReceiptNoteItemRepository extends JpaRepository<GoodsReceiptNoteItem, String> {
    List<GoodsReceiptNoteItem> findByGrnId(String grnId);
}
