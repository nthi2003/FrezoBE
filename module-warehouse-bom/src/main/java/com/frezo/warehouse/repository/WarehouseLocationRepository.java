package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.WarehouseLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocation, String> {
    List<WarehouseLocation> findByZoneId(String zoneId);
    Optional<WarehouseLocation> findByBarcode(String barcode);
}
