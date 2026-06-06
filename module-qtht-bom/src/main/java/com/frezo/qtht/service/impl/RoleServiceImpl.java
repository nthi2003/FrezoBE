package com.frezo.qtht.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.qtht.dto.request.RoleCreateRequest;
import com.frezo.qtht.dto.request.RoleUpdateRequest;
import com.frezo.qtht.dto.response.RoleResponse;
import com.frezo.qtht.entity.Role;
import com.frezo.qtht.mapper.RoleMapper;
import com.frezo.qtht.repository.RoleRepository;
import com.frezo.qtht.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleServiceImpl implements RoleService {

    private static final String STATUS_ACTIVE = "A";
    private static final String ERROR_ROLE_EXISTS = "exception.role.exists";
    private static final String ERROR_ROLE_NOT_FOUND = "exception.role.not.found";
    private static final String ERROR_ROLE_CONFLICT = "exception.role.conflict";

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Override
    public RoleResponse create(RoleCreateRequest request) {
        validateRoleUniqueness(request.getCode(), request.getAppCode());

        Role role = buildRoleFromRequest(request);
        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Override
    public RoleResponse update(RoleUpdateRequest request) {
        Role role = findRole(request);
        validateRoleUpdate(role, request);

        updateRoleFields(role, request);
        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Override
    public List<RoleResponse> getRoleByAppCode(String appCode) {
        return roleMapper.toResponseList(
                roleRepository.findByAppCodeAndIsDeletedFalse(appCode));
    }

    @Override
    public List<RoleResponse> getAll() {
        return roleMapper.toResponseList(roleRepository.findByIsDeletedFalse());
    }

    @Override
    @Transactional
    public void delete(String code, String appCode) {
        Role role = roleRepository
                .findByCodeAndAppCodeAndIsDeletedFalse(code, appCode)
                .orElseThrow(() -> new AppException(ERROR_ROLE_NOT_FOUND, HttpStatus.NOT_FOUND, code, appCode));

        role.setIsDeleted(true);
        roleRepository.save(role);
    }

    private void validateRoleUniqueness(String code, String appCode) {
        if (roleRepository.existsByCodeAndAppCodeAndIsDeletedFalse(code, appCode)) {
            throw new AppException(ERROR_ROLE_EXISTS, HttpStatus.BAD_REQUEST, code, appCode);
        }
    }

    private Role buildRoleFromRequest(RoleCreateRequest request) {
        Role role = Role.builder()
                .code(request.getCode())
                .appCode(request.getAppCode())
                .name(request.getName())
                .status(STATUS_ACTIVE)
                .build();

        role.setIsDeleted(false);

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }

        return role;
    }

    private Role findRole(RoleUpdateRequest request) {
        if (request.getId() != null) {
            return roleRepository.findById(request.getId())
                    .orElseThrow(() -> new AppException("exception.role.not.found", HttpStatus.NOT_FOUND, request.getId()));
        }

        return roleRepository.findByCodeAndAppCodeAndIsDeletedFalse(
                request.getCode(), request.getAppCode())
                .orElseThrow(() -> new AppException(ERROR_ROLE_NOT_FOUND, HttpStatus.NOT_FOUND, request.getCode(), request.getAppCode()));
    }

    private void validateRoleUpdate(Role role, RoleUpdateRequest request) {
        boolean codeChanged = !role.getCode().equals(request.getCode());
        boolean appCodeChanged = !role.getAppCode().equals(request.getAppCode());

        if (codeChanged || appCodeChanged) {
            if (roleRepository.existsByCodeAndAppCodeAndIsDeletedFalse(
                    request.getCode(), request.getAppCode())) {
                throw new AppException(ERROR_ROLE_CONFLICT, HttpStatus.CONFLICT);
            }
        }
    }

    private void updateRoleFields(Role role, RoleUpdateRequest request) {
        role.setCode(request.getCode());
        role.setAppCode(request.getAppCode());
        role.setName(request.getName());
        role.setDescription(request.getDescription());

        if (request.getStatus() != null) {
            role.setStatus(request.getStatus());
        }
    }
}
