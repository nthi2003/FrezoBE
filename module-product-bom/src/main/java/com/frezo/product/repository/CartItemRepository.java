package com.frezo.product.repository;

import com.frezo.product.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, String> {
    List<CartItem> findByCartId(String cartId);
    void deleteByCartId(String cartId);
}
