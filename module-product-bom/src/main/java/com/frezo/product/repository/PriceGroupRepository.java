package com.frezo.product.repository;

import com.frezo.product.entity.PriceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PriceGroupRepository extends JpaRepository<PriceGroup, String> {
    Optional<PriceGroup> findByCode(String code);
}
