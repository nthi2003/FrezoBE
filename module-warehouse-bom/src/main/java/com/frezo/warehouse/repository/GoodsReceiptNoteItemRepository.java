package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.GoodsReceiptNoteItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoodsReceiptNoteItemRepository extends JpaRepository<GoodsReceiptNoteItem, String> {
    List<GoodsReceiptNoteItem> findByGrnId(String grnId);

    /**
     * Lịch sử giá nhập NCC theo sản phẩm — join phiếu GRN (bỏ phiếu huỷ / soft-delete).
     */
    @Query("""
        SELECT i FROM GoodsReceiptNoteItem i
        JOIN FETCH i.goodsReceiptNote g
        WHERE i.productId = :productId
          AND i.unitCost IS NOT NULL
          AND (i.isDeleted IS NULL OR i.isDeleted = false)
          AND (g.isDeleted IS NULL OR g.isDeleted = false)
          AND (g.status IS NULL OR g.status <> 'CANCELLED')
        ORDER BY COALESCE(g.receivedAt, g.createdDate) ASC
        """)
    List<GoodsReceiptNoteItem> findPriceHistoryByProductId(@Param("productId") String productId);
}
