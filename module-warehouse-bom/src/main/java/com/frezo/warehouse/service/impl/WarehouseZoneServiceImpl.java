package com.frezo.warehouse.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.warehouse.common.WarehouseErrorCode;
import com.frezo.warehouse.dto.request.WarehouseZoneRequest;
import com.frezo.warehouse.dto.response.WarehouseZoneResponse;
import com.frezo.warehouse.entity.WarehouseZone;
import com.frezo.warehouse.repository.WarehouseZoneRepository;
import com.frezo.warehouse.service.WarehouseZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseZoneServiceImpl implements WarehouseZoneService {

    private final WarehouseZoneRepository zoneRepository;

    /** Lấy thông tin một khu vực (zone) của kho theo id. */
    @Override
    public WarehouseZoneResponse getById(String id) {
        WarehouseZone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.ZONE_NOT_FOUND));
        return toResponse(zone);
    }

    /** Lấy danh sách khu vực thuộc một kho. */
    @Override
    public List<WarehouseZoneResponse> getByWarehouseId(String warehouseId) {
        return zoneRepository.findByWarehouseId(warehouseId).stream().map(this::toResponse).toList();
    }

    /** Tạo mới một khu vực trong kho. */
    @Override
    @Transactional
    public WarehouseZoneResponse create(WarehouseZoneRequest request) {
        WarehouseZone zone = WarehouseZone.builder()
                .warehouseId(request.getWarehouseId())
                .code(request.getCode())
                .name(request.getName())
                .type(request.getType())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();
        zone = zoneRepository.save(zone);
        return toResponse(zone);
    }

    /** Cập nhật thông tin khu vực (chỉ các trường được cung cấp). */
    @Override
    @Transactional
    public WarehouseZoneResponse update(String id, WarehouseZoneRequest request) {
        WarehouseZone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.ZONE_NOT_FOUND));
        if (request.getCode() != null) zone.setCode(request.getCode());
        if (request.getName() != null) zone.setName(request.getName());
        if (request.getType() != null) zone.setType(request.getType());
        if (request.getStatus() != null) zone.setStatus(request.getStatus());
        if (request.getWarehouseId() != null) zone.setWarehouseId(request.getWarehouseId());
        zone = zoneRepository.save(zone);
        return toResponse(zone);
    }

    /** Xóa một khu vực theo id. */
    @Override
    @Transactional
    public void delete(String id) {
        WarehouseZone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.ZONE_NOT_FOUND));
        zoneRepository.delete(zone);
    }

    /** Chuyển entity khu vực thành response. */
    private WarehouseZoneResponse toResponse(WarehouseZone zone) {
        WarehouseZoneResponse r = new WarehouseZoneResponse();
        r.setId(zone.getId());
        r.setWarehouseId(zone.getWarehouseId());
        r.setCode(zone.getCode());
        r.setName(zone.getName());
        r.setType(zone.getType());
        r.setStatus(zone.getStatus());
        return r;
    }
}
