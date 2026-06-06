package com.frezo.product.repository;

import com.frezo.product.entity.MarketPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketPriceRepository extends JpaRepository<MarketPrice, String> {

    // So sánh giá của chúng ta với giá chợ đầu mối ngay hôm nay
    @Query(value = """
        SELECT p.name as productName,
               mp.market_name as marketName,
               mp.price as marketPrice,
               mp.unit as unit,
               (SELECT pc.price FROM price_configs pc
                JOIN product_units pu ON pu.id = pc.product_unit_id
                WHERE pu.product_id = p.id AND pu.unit_name = mp.unit
                  AND (pc.effective_date IS NULL OR pc.effective_date <= NOW())
                  AND (pc.expiry_date IS NULL OR pc.expiry_date >= NOW())
                LIMIT 1) as ourPrice
        FROM market_prices mp
        JOIN products p ON p.id = mp.product_id
        WHERE mp.price_date = CURRENT_DATE
        ORDER BY p.name ASC
        """, nativeQuery = true)
    List<Object[]> getTodayMarketComparison();
}
