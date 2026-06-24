package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, String>, JpaSpecificationExecutor<StockTransfer> {
    Optional<StockTransfer> findByTransferCode(String transferCode);
}
