package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.WarehouseZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseZoneRepository extends JpaRepository<WarehouseZone, String> {
    List<WarehouseZone> findByWarehouseId(String warehouseId);
}
