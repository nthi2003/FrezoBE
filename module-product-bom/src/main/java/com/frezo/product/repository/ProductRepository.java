package com.frezo.product.repository;

import com.frezo.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {
    java.util.Optional<Product> findByCode(String code);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true AND p.isDeleted = false")
    long countActiveProducts();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM Product p WHERE p.warningThreshold IS NOT NULL AND (SELECT SUM(b.currentQuantity) FROM Batch b WHERE b.productId = p.id) <= p.warningThreshold")
    long countLowStockProducts();
}
