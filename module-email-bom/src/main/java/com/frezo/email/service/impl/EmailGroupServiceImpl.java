package com.frezo.email.service.impl;

import com.frezo.email.dto.request.EmailGroupRequest;
import com.frezo.email.dto.response.EmailGroupResponse;
import com.frezo.email.entity.EmailGroup;
import com.frezo.email.mapper.EmailGroupMapper;
import com.frezo.email.repository.EmailGroupRepository;
import com.frezo.email.service.EmailGroupService;
import com.frezo.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailGroupServiceImpl implements EmailGroupService {

    private final EmailGroupRepository emailGroupRepository;
    private final EmailGroupMapper emailGroupMapper;

    /**
     * Lấy toàn bộ danh sách nhóm email.
     */
    @Override
    public List<EmailGroupResponse> getAll() {
        return emailGroupMapper.toResponseList(emailGroupRepository.findAll());
    }

    /**
     * Lấy thông tin nhóm email theo mã định danh.
     */
    @Override
    public EmailGroupResponse getById(String id) {
        EmailGroup group = emailGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Email group not found: " + id));
        return emailGroupMapper.toResponse(group);
    }

    /**
     * Tạo mới nhóm email từ dữ liệu yêu cầu.
     */
    @Override
    public ApiResponse<EmailGroupResponse> create(EmailGroupRequest request) {
        EmailGroup entity = emailGroupMapper.toEntity(request);
        entity = emailGroupRepository.save(entity);
        return ApiResponse.ok(emailGroupMapper.toResponse(entity));
    }

    /**
     * Cập nhật thông tin nhóm email theo mã định danh.
     */
    @Override
    public ApiResponse<EmailGroupResponse> update(String id, EmailGroupRequest request) {
        EmailGroup entity = emailGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Email group not found: " + id));
        emailGroupMapper.updateEntity(request, entity);
        entity = emailGroupRepository.save(entity);
        return ApiResponse.ok(emailGroupMapper.toResponse(entity));
    }

    /**
     * Xóa nhóm email theo mã định danh.
     */
    @Override
    public void delete(String id) {
        emailGroupRepository.deleteById(id);
    }
}
