package com.frezo.product.repository;

import com.frezo.product.entity.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, String> {

    // Tổng chi phí nhập kho (giá vốn) theo từng ngày trong N ngày gần nhất
    @Query(value = """
        SELECT DATE(il.created_at) as log_date,
               SUM(il.quantity * b.cost_price) as total_cost,
               SUM(il.quantity * b.cost_price * 1.3) as estimated_revenue
        FROM inventory_logs il
        JOIN batches b ON b.id = il.batch_id
        WHERE il.type = 'IMPORT'
          AND il.created_at >= :fromDate
        GROUP BY DATE(il.created_at)
        ORDER BY log_date ASC
        """, nativeQuery = true)
    List<Object[]> getProfitChartData(@Param("fromDate") LocalDate fromDate);
}

