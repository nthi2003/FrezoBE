package com.frezo.product.repository;

import com.frezo.product.entity.Carts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Carts, String> {
    Optional<Carts> findByCustomerIdAndStatus(String customerId, String status);
}
