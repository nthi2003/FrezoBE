package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockAlertRepository extends JpaRepository<StockAlert, String> {

    List<StockAlert> findByIsDeletedFalseOrderByTriggeredAtDesc();

    List<StockAlert> findByStatusAndIsDeletedFalseOrderByTriggeredAtDesc(String status);

    boolean existsByIdempotencyKeyAndIsDeletedFalse(String key);

    /**
     * Đã có cảnh báo đang mở cùng loại cho cặp sản phẩm + kho hay chưa.
     * <p>
     * Phải lọc theo {@code alertType}: một sản phẩm trong một kho có thể có nhiều cảnh báo
     * EXPIRY_SOON cùng lúc (mỗi lô hàng một cảnh báo), nên nếu bỏ điều kiện này thì
     * kiểm tra trùng của LOW_STOCK sẽ đụng phải nhiều bản ghi.
     * <p>
     * {@code alert_type} có thể NULL ở dữ liệu cũ (trước khi thêm cột) — coi như LOW_STOCK,
     * đồng bộ với cách hiển thị ở {@code toAlertDto}.
     */
    @Query("""
            SELECT COUNT(a) > 0 FROM StockAlert a
            WHERE a.productId = :productId
              AND a.warehouseId = :warehouseId
              AND a.status = :status
              AND COALESCE(a.alertType, 'LOW_STOCK') = :alertType
              AND (a.isDeleted = false OR a.isDeleted IS NULL)
            """)
    boolean existsOpenAlertOfType(@Param("productId") String productId,
                                  @Param("warehouseId") String warehouseId,
                                  @Param("alertType") String alertType,
                                  @Param("status") String status);
}
