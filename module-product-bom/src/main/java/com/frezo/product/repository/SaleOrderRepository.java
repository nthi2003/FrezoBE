package com.frezo.product.repository;

import com.frezo.product.entity.SaleOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SaleOrderRepository extends JpaRepository<SaleOrder, String> {
    Optional<SaleOrder> findByOrderCode(String orderCode);

    long countByCreatedDateBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT SUM(s.totalAmount) FROM SaleOrder s WHERE s.createdDate BETWEEN :from AND :to")
    Double sumTotalAmountByCreatedDateBetween(LocalDateTime from, LocalDateTime to);
}
