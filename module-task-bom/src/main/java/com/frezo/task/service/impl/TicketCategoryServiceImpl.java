package com.frezo.task.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.task.dto.request.TicketCategoryRequest;
import com.frezo.task.dto.response.TicketCategoryResponse;
import com.frezo.task.entity.TicketCategory;
import com.frezo.task.mapper.TicketCategoryMapper;
import com.frezo.task.repository.TicketCategoryRepository;
import com.frezo.task.service.TicketCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TicketCategoryServiceImpl implements TicketCategoryService {

    private static final String ERROR_CODE_EXISTS = "TICKET_CATEGORY_CODE_EXISTS";
    private static final String ERROR_NOT_FOUND = "TICKET_CATEGORY_NOT_FOUND";

    private final TicketCategoryMapper ticketCategoryMapper;
    private final TicketCategoryRepository ticketCategoryRepository;

    @Override
    @Transactional
    public TicketCategoryResponse add(TicketCategoryRequest request) {
        validateRequest(request);
        if (ticketCategoryRepository.existsByCodeAndIsDeletedFalse(normalizeCode(request.getCode()))) {
            throw new QTHTException(ERROR_CODE_EXISTS, "Mã danh mục đã tồn tại: " + request.getCode());
        }
        TicketCategory entity = ticketCategoryMapper.toEntity(request);
        entity.setCode(normalizeCode(request.getCode()));
        entity.setIsDeleted(false);
        if (entity.getActive() == null) {
            entity.setActive(true);
        }
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        return ticketCategoryMapper.toResponse(ticketCategoryRepository.save(entity));
    }

    @Override
    @Transactional
    public TicketCategoryResponse edit(String id, TicketCategoryRequest request) {
        TicketCategory exist = findEntityById(id);
        String newCode = normalizeCode(request.getCode());
        if (!Objects.equals(exist.getCode(), newCode)) {
            if (ticketCategoryRepository.existsByCodeAndIsDeletedFalse(newCode)) {
                throw new QTHTException(ERROR_CODE_EXISTS, "Mã danh mục đã tồn tại: " + newCode);
            }
        }
        validateRequest(request);
        ticketCategoryMapper.updateEntity(request, exist);
        exist.setCode(newCode);
        if (exist.getActive() == null) {
            exist.setActive(true);
        }
        return ticketCategoryMapper.toResponse(ticketCategoryRepository.save(exist));
    }

    @Override
    @Transactional
    public Void delete(String id) {
        TicketCategory entity = findEntityById(id);
        entity.setIsDeleted(true);
        entity.setActive(false);
        ticketCategoryRepository.save(entity);
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketCategoryResponse> findAll() {
        return ticketCategoryMapper.toResponseList(
                ticketCategoryRepository.findByIsDeletedFalseOrderBySortOrderAscNameAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketCategoryResponse> findActive() {
        return ticketCategoryMapper.toResponseList(
                ticketCategoryRepository.findByActiveTrueAndIsDeletedFalseOrderBySortOrderAscNameAsc());
    }

    private void validateRequest(TicketCategoryRequest request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new QTHTException("TICKET_CATEGORY_CODE_REQUIRED", "Mã danh mục bắt buộc");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new QTHTException("TICKET_CATEGORY_NAME_REQUIRED", "Tên danh mục bắt buộc");
        }
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim();
    }

    @Transactional(readOnly = true)
    protected TicketCategory findEntityById(String id) {
        return ticketCategoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new QTHTException(ERROR_NOT_FOUND, "Không tìm thấy danh mục: " + id));
    }
}
