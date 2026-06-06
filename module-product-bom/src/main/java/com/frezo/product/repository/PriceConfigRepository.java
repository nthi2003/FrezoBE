package com.frezo.product.repository;

import com.frezo.product.entity.PriceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PriceConfigRepository extends JpaRepository<PriceConfig, String> {
    List<PriceConfig> findByProductUnitId(String productUnitId);

    @Query("SELECT pc FROM PriceConfig pc WHERE pc.productUnitId = :unitId AND pc.priceGroupId = :groupId AND (pc.effectiveDate IS NULL OR pc.effectiveDate <= CURRENT_TIMESTAMP) AND (pc.expiryDate IS NULL OR pc.expiryDate >= CURRENT_TIMESTAMP)")
    List<PriceConfig> findActivePrice(String unitId, String groupId);

    // Top sản phẩm biến động giá so với ngày hôm qua
    @Query(value = """
        SELECT p.name as productName,
               today_pc.price as todayPrice,
               yday_pc.price as yesterdayPrice,
               ROUND((today_pc.price - yday_pc.price) / yday_pc.price * 100, 1) as changePct
        FROM price_configs today_pc
        JOIN product_units pu ON pu.id = today_pc.product_unit_id
        JOIN products p ON p.id = pu.product_id
        JOIN price_configs yday_pc
          ON yday_pc.product_unit_id = today_pc.product_unit_id
         AND DATE(yday_pc.created_at) = CURRENT_DATE - INTERVAL '1 day'
        WHERE DATE(today_pc.created_at) = CURRENT_DATE
          AND yday_pc.price > 0
        ORDER BY ABS((today_pc.price - yday_pc.price) / yday_pc.price) DESC
        LIMIT 10
        """, nativeQuery = true)
    List<Object[]> getPriceFluctuation();
}
