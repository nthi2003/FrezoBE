package com.frezo.warehouse.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.response.PageResponse;
import com.frezo.warehouse.dto.request.WarehouseCreateRequest;
import com.frezo.warehouse.dto.request.WarehouseUpdateRequest;
import com.frezo.warehouse.dto.response.WarehouseResponse;
import com.frezo.warehouse.entity.Warehouse;
import com.frezo.warehouse.repository.WarehouseRepository;
import com.frezo.warehouse.service.WarehouseService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Override
    public WarehouseResponse getById(String id) {
        Warehouse wh = warehouseRepository.findById(id)
                .orElseThrow(() -> new QTHTException("warehouse.not.found", HttpStatus.NOT_FOUND));
        return toResponse(wh);
    }

    @Override
    public WarehouseResponse getByCode(String code) {
        Warehouse wh = warehouseRepository.findByCode(code)
                .orElseThrow(() -> new QTHTException("warehouse.not.found", HttpStatus.NOT_FOUND));
        return toResponse(wh);
    }

    @Override
    public List<WarehouseResponse> getAll() {
        return warehouseRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public PageResponse<WarehouseResponse> filter(String keyword, String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Warehouse> whPage = warehouseRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (keyword != null && !keyword.isBlank()) {
                Predicate nameLike = cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%");
                Predicate codeLike = cb.like(cb.lower(root.get("code")), "%" + keyword.toLowerCase() + "%");
                predicates.add(cb.or(nameLike, codeLike));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
        List<WarehouseResponse> responses = whPage.getContent().stream().map(this::toResponse).toList();
        return PageResponse.of(page, size, whPage, responses);
    }

    @Override
    @Transactional
    public WarehouseResponse create(WarehouseCreateRequest request) {
        if (warehouseRepository.findByCode(request.getCode()).isPresent()) {
            throw new QTHTException("warehouse.code.exists", HttpStatus.BAD_REQUEST);
        }
        Warehouse wh = Warehouse.builder()
                .code(request.getCode())
                .name(request.getName())
                .shortName(request.getShortName())
                .type(request.getType())
                .addressLine(request.getAddressLine())
                .ward(request.getWard())
                .district(request.getDistrict())
                .province(request.getProvince())
                .countryCode(request.getCountryCode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .managerId(request.getManagerId())
                .phone(request.getPhone())
                .email(request.getEmail())
                .totalAreaSqm(request.getTotalAreaSqm())
                .maxCapacity(request.getMaxCapacity())
                .capacityUnit(request.getCapacityUnit())
                .temperatureMin(request.getTemperatureMin())
                .temperatureMax(request.getTemperatureMax())
                .isColdStorage(request.getIsColdStorage() != null ? request.getIsColdStorage() : false)
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .note(request.getNote())
                .build();
        wh = warehouseRepository.save(wh);
        return toResponse(wh);
    }

    @Override
    @Transactional
    public WarehouseResponse update(String id, WarehouseUpdateRequest request) {
        Warehouse wh = warehouseRepository.findById(id)
                .orElseThrow(() -> new QTHTException("warehouse.not.found", HttpStatus.NOT_FOUND));
        if (request.getName() != null) wh.setName(request.getName());
        if (request.getShortName() != null) wh.setShortName(request.getShortName());
        if (request.getType() != null) wh.setType(request.getType());
        if (request.getAddressLine() != null) wh.setAddressLine(request.getAddressLine());
        if (request.getWard() != null) wh.setWard(request.getWard());
        if (request.getDistrict() != null) wh.setDistrict(request.getDistrict());
        if (request.getProvince() != null) wh.setProvince(request.getProvince());
        if (request.getCountryCode() != null) wh.setCountryCode(request.getCountryCode());
        if (request.getLatitude() != null) wh.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) wh.setLongitude(request.getLongitude());
        if (request.getManagerId() != null) wh.setManagerId(request.getManagerId());
        if (request.getPhone() != null) wh.setPhone(request.getPhone());
        if (request.getEmail() != null) wh.setEmail(request.getEmail());
        if (request.getTotalAreaSqm() != null) wh.setTotalAreaSqm(request.getTotalAreaSqm());
        if (request.getMaxCapacity() != null) wh.setMaxCapacity(request.getMaxCapacity());
        if (request.getCapacityUnit() != null) wh.setCapacityUnit(request.getCapacityUnit());
        if (request.getTemperatureMin() != null) wh.setTemperatureMin(request.getTemperatureMin());
        if (request.getTemperatureMax() != null) wh.setTemperatureMax(request.getTemperatureMax());
        if (request.getIsColdStorage() != null) wh.setIsColdStorage(request.getIsColdStorage());
        if (request.getStatus() != null) wh.setStatus(request.getStatus());
        if (request.getIsDefault() != null) wh.setIsDefault(request.getIsDefault());
        if (request.getNote() != null) wh.setNote(request.getNote());
        wh = warehouseRepository.save(wh);
        return toResponse(wh);
    }

    @Override
    @Transactional
    public void delete(String id) {
        Warehouse wh = warehouseRepository.findById(id)
                .orElseThrow(() -> new QTHTException("warehouse.not.found", HttpStatus.NOT_FOUND));
        warehouseRepository.delete(wh);
    }

    private WarehouseResponse toResponse(Warehouse wh) {
        WarehouseResponse r = new WarehouseResponse();
        r.setId(wh.getId());
        r.setCode(wh.getCode());
        r.setName(wh.getName());
        r.setShortName(wh.getShortName());
        r.setType(wh.getType());
        r.setAddressLine(wh.getAddressLine());
        r.setWard(wh.getWard());
        r.setDistrict(wh.getDistrict());
        r.setProvince(wh.getProvince());
        r.setCountryCode(wh.getCountryCode());
        r.setManagerId(wh.getManagerId());
        r.setPhone(wh.getPhone());
        r.setEmail(wh.getEmail());
        r.setTotalAreaSqm(wh.getTotalAreaSqm());
        r.setMaxCapacity(wh.getMaxCapacity());
        r.setCapacityUnit(wh.getCapacityUnit());
        r.setTemperatureMin(wh.getTemperatureMin());
        r.setTemperatureMax(wh.getTemperatureMax());
        r.setIsColdStorage(wh.getIsColdStorage());
        r.setStatus(wh.getStatus());
        r.setIsDefault(wh.getIsDefault());
        r.setNote(wh.getNote());
        return r;
    }
}
