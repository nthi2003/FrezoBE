package com.frezo.task.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.task.common.TaskErrorCode;
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

    private final TicketCategoryMapper ticketCategoryMapper;
    private final TicketCategoryRepository ticketCategoryRepository;

    /**
     * Thêm mới danh mục ticket, kiểm tra trùng mã và mã hóa dữ liệu.
     */
    @Override
    @Transactional
    public TicketCategoryResponse add(TicketCategoryRequest request) {
        validateRequest(request);
        if (ticketCategoryRepository.existsByCodeAndIsDeletedFalse(normalizeCode(request.getCode()))) {
            throw new AppException(TaskErrorCode.TICKET_CATEGORY_CODE_EXISTS, request.getCode());
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

    /**
     * Cập nhật danh mục ticket, kiểm tra trùng mã nếu mã thay đổi.
     */
    @Override
    @Transactional
    public TicketCategoryResponse edit(String id, TicketCategoryRequest request) {
        TicketCategory exist = findEntityById(id);
        String newCode = normalizeCode(request.getCode());
        if (!Objects.equals(exist.getCode(), newCode)) {
            if (ticketCategoryRepository.existsByCodeAndIsDeletedFalse(newCode)) {
                throw new AppException(TaskErrorCode.TICKET_CATEGORY_CODE_EXISTS, newCode);
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

    /**
     * Xóa mềm danh mục ticket và tắt trạng thái kích hoạt.
     */
    @Override
    @Transactional
    public Void delete(String id) {
        TicketCategory entity = findEntityById(id);
        entity.setIsDeleted(true);
        entity.setActive(false);
        ticketCategoryRepository.save(entity);
        return null;
    }

    /**
     * Lấy tất cả danh mục ticket chưa xóa, sắp xếp theo thứ tự và tên.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TicketCategoryResponse> findAll() {
        return ticketCategoryMapper.toResponseList(
                ticketCategoryRepository.findByIsDeletedFalseOrderBySortOrderAscNameAsc());
    }

    /**
     * Lấy danh sách danh mục ticket đang kích hoạt.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TicketCategoryResponse> findActive() {
        return ticketCategoryMapper.toResponseList(
                ticketCategoryRepository.findByActiveTrueAndIsDeletedFalseOrderBySortOrderAscNameAsc());
    }

    /** Kiểm tra mã và tên danh mục không được để trống. */
    private void validateRequest(TicketCategoryRequest request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new AppException(TaskErrorCode.TICKET_CATEGORY_CODE_REQUIRED);
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new AppException(TaskErrorCode.TICKET_CATEGORY_NAME_REQUIRED);
        }
    }

    /** Chuẩn hóa mã danh mục bằng cách loại bỏ khoảng trắng thừa. */
    private String normalizeCode(String code) {
        return code == null ? null : code.trim();
    }

    /** Tìm entity danh mục theo mã, ném ngoại lệ nếu không tồn tại hoặc đã bị xóa. */
    @Transactional(readOnly = true)
    protected TicketCategory findEntityById(String id) {
        return ticketCategoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(TaskErrorCode.TICKET_CATEGORY_NOT_FOUND, id));
    }
}
