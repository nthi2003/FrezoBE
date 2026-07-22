package com.frezo.qtbv.repository;

import com.frezo.qtbv.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, String>, JpaSpecificationExecutor<Asset> {

    Optional<Asset> findByCode(String code);

    boolean existsByCode(String code);

    /** Cho auto-gen code: đếm asset đã tạo trong năm để suffix number. */
    @Query("SELECT COUNT(a) FROM Asset a WHERE a.code LIKE :prefix%")
    long countByCodePrefix(String prefix);
}
