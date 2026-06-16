package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, String>, JpaSpecificationExecutor<Warehouse> {
    Optional<Warehouse> findByCode(String code);
    List<Warehouse> findByStatus(String status);
    Optional<Warehouse> findByIsDefaultTrue();
}
