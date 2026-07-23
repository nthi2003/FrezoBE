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

    @Override
    public List<EmailGroupResponse> getAll() {
        return emailGroupMapper.toResponseList(emailGroupRepository.findAll());
    }

    @Override
    public EmailGroupResponse getById(String id) {
        EmailGroup group = emailGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Email group not found: " + id));
        return emailGroupMapper.toResponse(group);
    }

    @Override
    public ApiResponse<EmailGroupResponse> create(EmailGroupRequest request) {
        EmailGroup entity = emailGroupMapper.toEntity(request);
        entity = emailGroupRepository.save(entity);
        return ApiResponse.ok(emailGroupMapper.toResponse(entity));
    }

    @Override
    public ApiResponse<EmailGroupResponse> update(String id, EmailGroupRequest request) {
        EmailGroup entity = emailGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Email group not found: " + id));
        emailGroupMapper.updateEntity(request, entity);
        entity = emailGroupRepository.save(entity);
        return ApiResponse.ok(emailGroupMapper.toResponse(entity));
    }

    @Override
    public void delete(String id) {
        emailGroupRepository.deleteById(id);
    }
}
