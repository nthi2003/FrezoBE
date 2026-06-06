package com.frezo.product.repository;

import com.frezo.product.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BatchRepository extends JpaRepository<Batch, String> {
    List<Batch> findByProductIdAndCurrentQuantityGreaterThanOrderByExpiryDateAsc(String productId, Double quantity);
    
    List<Batch> findByBatchCode(String batchCode);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(b) FROM Batch b WHERE b.expiryDate IS NOT NULL AND b.expiryDate <= :date AND b.currentQuantity > 0")
    long countExpiringSoon(@org.springframework.data.repository.query.Param("date") java.time.LocalDate date);
}
