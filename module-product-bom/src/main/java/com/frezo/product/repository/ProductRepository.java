package com.frezo.product.repository;

import com.frezo.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {
    java.util.Optional<Product> findByCode(String code);

    @Query("SELECT MAX(p.code) FROM Product p")
    String findMaxCode();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true AND p.isDeleted = false")
    long countActiveProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.warningThreshold IS NOT NULL AND (SELECT SUM(b.currentQuantity) FROM Batch b WHERE b.productId = p.id) <= p.warningThreshold")
    long countLowStockProducts();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p SET p.viewCount = COALESCE(p.viewCount, 0) + 1 WHERE p.id = :id")
    int incrementViewCount(@Param("id") String id);
}
