package com.frezo.product.repository;

import com.frezo.product.entity.SaleOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SaleOrderRepository extends JpaRepository<SaleOrder, String> {
    Optional<SaleOrder> findByOrderCode(String orderCode);
}
