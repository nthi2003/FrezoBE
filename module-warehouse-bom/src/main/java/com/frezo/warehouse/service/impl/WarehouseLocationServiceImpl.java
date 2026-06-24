package com.frezo.warehouse.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.warehouse.dto.request.WarehouseLocationRequest;
import com.frezo.warehouse.dto.response.WarehouseLocationResponse;
import com.frezo.warehouse.entity.WarehouseLocation;
import com.frezo.warehouse.repository.WarehouseLocationRepository;
import com.frezo.warehouse.service.WarehouseLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseLocationServiceImpl implements WarehouseLocationService {

    private final WarehouseLocationRepository locationRepository;

    @Override
    public WarehouseLocationResponse getById(String id) {
        WarehouseLocation loc = locationRepository.findById(id)
                .orElseThrow(() -> new QTHTException("warehouse.location.not.found", HttpStatus.NOT_FOUND));
        return toResponse(loc);
    }

    @Override
    public List<WarehouseLocationResponse> getByZoneId(String zoneId) {
        return locationRepository.findByZoneId(zoneId).stream().map(this::toResponse).toList();
    }

    @Override
    public WarehouseLocationResponse getByBarcode(String barcode) {
        WarehouseLocation loc = locationRepository.findByBarcode(barcode)
                .orElseThrow(() -> new QTHTException("warehouse.location.not.found", HttpStatus.NOT_FOUND));
        return toResponse(loc);
    }

    @Override
    @Transactional
    public WarehouseLocationResponse create(WarehouseLocationRequest request) {
        WarehouseLocation loc = WarehouseLocation.builder()
                .zoneId(request.getZoneId())
                .aisle(request.getAisle())
                .rack(request.getRack())
                .level(request.getLevel())
                .bin(request.getBin())
                .barcode(request.getBarcode())
                .maxWeightKg(request.getMaxWeightKg())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        loc = locationRepository.save(loc);
        return toResponse(loc);
    }

    @Override
    @Transactional
    public WarehouseLocationResponse update(String id, WarehouseLocationRequest request) {
        WarehouseLocation loc = locationRepository.findById(id)
                .orElseThrow(() -> new QTHTException("warehouse.location.not.found", HttpStatus.NOT_FOUND));
        if (request.getZoneId() != null) loc.setZoneId(request.getZoneId());
        if (request.getAisle() != null) loc.setAisle(request.getAisle());
        if (request.getRack() != null) loc.setRack(request.getRack());
        if (request.getLevel() != null) loc.setLevel(request.getLevel());
        if (request.getBin() != null) loc.setBin(request.getBin());
        if (request.getBarcode() != null) loc.setBarcode(request.getBarcode());
        if (request.getMaxWeightKg() != null) loc.setMaxWeightKg(request.getMaxWeightKg());
        if (request.getIsActive() != null) loc.setIsActive(request.getIsActive());
        loc = locationRepository.save(loc);
        return toResponse(loc);
    }

    @Override
    @Transactional
    public void delete(String id) {
        WarehouseLocation loc = locationRepository.findById(id)
                .orElseThrow(() -> new QTHTException("warehouse.location.not.found", HttpStatus.NOT_FOUND));
        locationRepository.delete(loc);
    }

    private WarehouseLocationResponse toResponse(WarehouseLocation loc) {
        WarehouseLocationResponse r = new WarehouseLocationResponse();
        r.setId(loc.getId());
        r.setZoneId(loc.getZoneId());
        r.setAisle(loc.getAisle());
        r.setRack(loc.getRack());
        r.setLevel(loc.getLevel());
        r.setBin(loc.getBin());
        r.setBarcode(loc.getBarcode());
        r.setMaxWeightKg(loc.getMaxWeightKg());
        r.setIsActive(loc.getIsActive());
        return r;
    }
}
