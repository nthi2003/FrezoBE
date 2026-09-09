package com.frezo.fbautomation.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.fbautomation.common.FbAutomationErrorCode;
import com.frezo.fbautomation.dto.response.FacebookGroupResponse;
import com.frezo.fbautomation.entity.FacebookGroup;
import com.frezo.fbautomation.mapper.FacebookGroupMapper;
import com.frezo.fbautomation.repository.FacebookGroupRepository;
import com.frezo.fbautomation.service.FacebookGroupService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FacebookGroupServiceImpl implements FacebookGroupService {

    private final FacebookGroupRepository groupRepository;
    private final FacebookGroupMapper groupMapper;

    /** Lấy danh sách nhóm Facebook, có thể lọc theo trạng thái ("all" để lấy tất cả). */
    @Override
    public List<FacebookGroupResponse> getAll(String status) {
        List<FacebookGroup> groups;
        if (status == null || status.isBlank() || "all".equals(status)) {
            groups = groupRepository.findAll();
        } else {
            groups = groupRepository.findByStatus(status);
        }
        return groups.stream().map(groupMapper::toResponse).toList();
    }

    /** Lấy thông tin chi tiết nhóm Facebook theo mã định danh. */
    @Override
    public FacebookGroupResponse getById(String id) {
        return groupMapper.toResponse(findById(id));
    }

    /** Xóa nhóm Facebook theo mã định danh. */
    @Override
    @Transactional
    public void delete(String id) {
        findById(id);
        groupRepository.deleteById(id);
    }

    /** Đếm số nhóm Facebook theo trạng thái. */
    @Override
    public long countByStatus(String status) {
        return groupRepository.findByStatus(status).size();
    }

    /** Tìm nhóm Facebook theo mã, ném ngoại lệ nếu không tồn tại. */
    private FacebookGroup findById(String id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new AppException(FbAutomationErrorCode.GROUP_NOT_FOUND));
    }
}
