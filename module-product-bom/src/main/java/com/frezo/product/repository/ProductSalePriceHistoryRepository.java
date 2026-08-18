package com.frezo.product.repository;

import com.frezo.product.entity.ProductSalePriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSalePriceHistoryRepository extends JpaRepository<ProductSalePriceHistory, String> {

    List<ProductSalePriceHistory> findByProductIdAndIsDeletedFalseOrderByCreatedDateDesc(String productId);
}
