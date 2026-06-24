package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.GoodsReceiptNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsReceiptNoteRepository extends JpaRepository<GoodsReceiptNote, String>, JpaSpecificationExecutor<GoodsReceiptNote> {
    Optional<GoodsReceiptNote> findByGrnCode(String grnCode);
    List<GoodsReceiptNote> findByStatus(String status);
    List<GoodsReceiptNote> findByPurchaseOrderId(String purchaseOrderId);
    long countByCreatedDateBetween(LocalDateTime from, LocalDateTime to);
}
