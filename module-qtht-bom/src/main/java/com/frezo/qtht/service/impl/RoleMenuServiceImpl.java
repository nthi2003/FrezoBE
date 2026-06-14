package com.frezo.qtht.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.qtht.dto.request.RoleMenuAssignRequest;
import com.frezo.qtht.dto.request.RolePermissionSaveRequest;
import com.frezo.qtht.dto.response.RoleMenuResponse;

import com.frezo.qtht.entity.Menu;
import com.frezo.qtht.entity.Role;
import com.frezo.qtht.entity.RoleMenu;
import com.frezo.qtht.mapper.RoleMenuMapper;

import com.frezo.qtht.repository.RoleMenuRepository;
import com.frezo.qtht.repository.RoleRepository;
import com.frezo.qtht.service.RoleMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleMenuServiceImpl implements RoleMenuService {

    private static final String ERR_ROLE_NOT_FOUND = "exception.role.not.found";

    private final RoleMenuRepository roleMenuRepository;
    private final RoleMenuMapper roleMenuMapper;
    private final RoleRepository roleRepository;
    private final com.frezo.qtht.repository.MenuRepository menuRepository;

    @Override
    @Transactional
    public RoleMenuResponse assignMenuToRole(RoleMenuAssignRequest request) {
        if (request == null) {
            throw new AppException("error.invalid.request", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getRoleCode())) {
            throw new AppException("exception.role.not.found", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getApi())) {
            throw new AppException("error.api.required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getAction())) {
            throw new AppException("error.action.required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getAppCode())) {
            throw new AppException("error.appCode.required", HttpStatus.BAD_REQUEST);
        }

        // Find role
        Role role = roleRepository.findByCodeAndAppCodeAndIsDeletedFalse(request.getRoleCode(), request.getAppCode())
                .orElseThrow(() -> new AppException(ERR_ROLE_NOT_FOUND, HttpStatus.NOT_FOUND));

        // Find menu by api/code + appCode
        Menu menu = menuRepository.findByCodeAndAppCode(request.getApi(), request.getAppCode())
                .orElseThrow(() -> new AppException("exception.menu.not.found", HttpStatus.NOT_FOUND));

        // Prevent duplicates (unique: role_id, menu_id, app_code)
        List<RoleMenu> existing = roleMenuRepository.findByRole_Code(role.getCode());
        Optional<RoleMenu> existedOpt = existing.stream()
                .filter(rm -> rm.getMenu() != null && rm.getMenu().getId() != null)
                .filter(rm -> request.getApi().equals(rm.getMenu().getCode()))
                .filter(rm -> request.getAppCode().equals(rm.getAppCode()))
                .findFirst();

        if (existedOpt.isPresent()) {
            RoleMenu existed = existedOpt.get();
            existed.setAction(request.getAction());
            roleMenuRepository.save(existed);
            return roleMenuMapper.toResponseDTO(existed);
        }

        // Parent menu id (if menu is a child)
        Map<String, Menu> parentMenuMap = fetchParentMenus(Collections.singletonList(menu), role.getAppCode());
        RoleMenu roleMenu = RoleMenu.builder()
                .role(role)
                .menu(menu)
                .appCode(role.getAppCode())
                .action(request.getAction())
                .build();

        if (StringUtils.hasText(menu.getParentCode())) {
            Menu parent = parentMenuMap.get(menu.getParentCode());
            if (parent != null) {
                roleMenu.setParentMenuId(parent.getId());
            }
        }

        roleMenu.setId(UUID.randomUUID().toString());
        RoleMenu saved = roleMenuRepository.save(roleMenu);
        return roleMenuMapper.toResponseDTO(saved);
    }


    @Override
    @Transactional(readOnly = true)
    public List<RoleMenuResponse> getMenusByRole(String roleCode) {

        Role role = roleRepository.findByIsDeletedFalse().stream()
                .filter(r -> r.getCode().equals(roleCode))
                .findFirst()
                .orElse(null);

        if (role == null) {
            return Collections.emptyList();
        }

        List<RoleMenu> entities = roleMenuRepository.findByRole_IdIn(Collections.singletonList(role.getId()));

        return entities.stream()
                .map(roleMenuMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public void savePermissions(RolePermissionSaveRequest request) {
        Role role = getRoleById(request.getRoleId());
        List<String> effectiveMenuIds = request.getMenuIds() != null ? request.getMenuIds() : Collections.emptyList();

        deleteRemovedPermissions(role, effectiveMenuIds);
        addNewPermissions(role, effectiveMenuIds);
    }


    private Role getRoleById(String roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ERR_ROLE_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    private void deleteRemovedPermissions(Role role, List<String> effectiveMenuIds) {
        List<RoleMenu> existingRoleMenus = roleMenuRepository.findByRole_Code(role.getCode());
        List<RoleMenu> toDelete = existingRoleMenus.stream()
                .filter(rm -> rm.getMenu() != null && !effectiveMenuIds.contains(rm.getMenu().getCode()))
                .toList();

        if (!toDelete.isEmpty()) {
            roleMenuRepository.deleteAll(toDelete);
        }
    }

    private void addNewPermissions(Role role, List<String> effectiveMenuIds) {
        List<RoleMenu> existingRoleMenus = roleMenuRepository.findByRole_Code(role.getCode());
        Set<String> existingMenuCodes = existingRoleMenus.stream()
                .map(rm -> rm.getMenu().getCode())
                .collect(Collectors.toSet());

        List<String> toAddMenuCodes = effectiveMenuIds.stream()
                .filter(code -> !existingMenuCodes.contains(code))
                .toList();

        if (toAddMenuCodes.isEmpty()) {
            return;
        }

        List<Menu> menusToAdd = menuRepository.findByCodeIn(toAddMenuCodes);
        Map<String, Menu> parentMenuMap = fetchParentMenus(menusToAdd, role.getAppCode());

        List<RoleMenu> newRoleMenus = menusToAdd.stream()
                .map(menu -> createRoleMenu(role, menu, parentMenuMap))
                .toList();

        roleMenuRepository.saveAll(newRoleMenus);
    }

    private Map<String, Menu> fetchParentMenus(List<Menu> menus, String appCode) {
        Set<String> parentCodes = menus.stream()
                .map(Menu::getParentCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        if (parentCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Menu> allAppMenus = menuRepository.findByAppCode(appCode);
        return allAppMenus.stream()
                .filter(m -> parentCodes.contains(m.getCode()))
                .collect(Collectors.toMap(Menu::getCode, Function.identity(), (a, b) -> a));
    }

    private RoleMenu createRoleMenu(Role role, Menu menu, Map<String, Menu> parentMenuMap) {
        RoleMenu.RoleMenuBuilder builder = RoleMenu.builder()
                .role(role)
                .menu(menu)
                .appCode(role.getAppCode())
                .action("ALL");

        if (StringUtils.hasText(menu.getParentCode())) {
            Menu parent = parentMenuMap.get(menu.getParentCode());
            if (parent != null) {
                builder.parentMenuId(parent.getId());
            }
        }

        RoleMenu roleMenu = builder.build();
        roleMenu.setId(UUID.randomUUID().toString());
        return roleMenu;
    }
}
