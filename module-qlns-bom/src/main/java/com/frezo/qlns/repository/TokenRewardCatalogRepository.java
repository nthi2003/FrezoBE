package com.frezo.qlns.repository;

import com.frezo.qlns.entity.TokenRewardCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TokenRewardCatalogRepository extends JpaRepository<TokenRewardCatalog, String> {
    List<TokenRewardCatalog> findByActiveTrueAndIsDeletedFalseOrderByTokenCostAsc();
}
