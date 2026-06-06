package com.frezo.product.repository;

import com.frezo.product.entity.ProductUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductUnitRepository extends JpaRepository<ProductUnit, String>, JpaSpecificationExecutor<ProductUnit> {
    List<ProductUnit> findByProductId(String productId);
}
