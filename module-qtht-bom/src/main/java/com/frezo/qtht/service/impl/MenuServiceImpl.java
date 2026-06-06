package com.frezo.qtht.service.impl;

import com.frezo.auth.entity.User;
import com.frezo.auth.entity.UserRole;
import com.frezo.auth.repository.UserRepository;
import com.frezo.auth.repository.UserRoleRepository;
import com.frezo.common.exception.AppException;
import com.frezo.qtht.dto.request.MenuSaveRequest;
import com.frezo.qtht.dto.response.MenuResponse;
import com.frezo.qtht.entity.Menu;
import com.frezo.qtht.entity.MenuPermission;
import com.frezo.qtht.mapper.MenuMapper;
import com.frezo.qtht.repository.MenuPermissionRepository;
import com.frezo.qtht.repository.MenuRepository;
import com.frezo.qtht.repository.PersonRepository;
import com.frezo.qtht.repository.RoleMenuRepository;
import com.frezo.qtht.repository.RoleRepository;
import com.frezo.qtht.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private static final String CACHE_NAME = "userMenus";
    private static final String ERROR_MENU_NOT_FOUND = "exception.menu.not.found";
    private static final String ERROR_MENU_CODE_EXISTS = "exception.menu.code.exists";

    private final UserRoleRepository userRoleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final MenuRepository menuRepository;
    private final MenuPermissionRepository menuPermissionRepository;
    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final RoleRepository roleRepository;
    private final MenuMapper menuMapper;

    @Override
    @Cacheable(value = CACHE_NAME, key = "#username", unless = "#result == null || #result.isEmpty()")
    public List<MenuResponse> getMenusForUser(String username) {

        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new AppException("exception.user.not_found", username));

        boolean isAdmin = false;
        if (user.getPersonId() != null) {
            isAdmin = personRepository.findByIdAndIsDeletedFalse(user.getPersonId())
                    .map(person -> {
                        return Boolean.TRUE.equals(person.getIsAdmin());
                    })
                    .orElse(false);
        } else if (user.getEmail() != null) {
            isAdmin = personRepository.findByEmail(user.getEmail())
                    .map(person -> {
                        return Boolean.TRUE.equals(person.getIsAdmin());
                    })
                    .orElse(false);
        }

        if (!isAdmin && "admin".equalsIgnoreCase(username)) {
            isAdmin = true;
        }


        if (isAdmin) {
            List<Menu> allMenus = menuRepository.findByIsDeletedFalse();
            return menuMapper.toSidebarResponseList(allMenus);
        }

        List<UserRole> userRoles = userRoleRepository.findByUserIdAndIsDeletedFalse(user.getId());
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .distinct()
                .toList();

        List<com.frezo.qtht.entity.Role> roles = roleRepository.findAllById(roleIds);
        List<String> roleCodes = roles.stream()
                .map(com.frezo.qtht.entity.Role::getCode)
                .toList();


        if (roleCodes.stream()
                .anyMatch(role -> "ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role))) {
            isAdmin = true;
        }

        if (isAdmin) {
            List<Menu> allMenus = menuRepository.findByIsDeletedFalse();
            return menuMapper.toSidebarResponseList(allMenus);
        }

        List<Menu> menus = roleMenuRepository.findMenusByRoleCodes(roleCodes);

        return menuMapper.toSidebarResponseList(menus);
    }

    @Override
    public List<MenuResponse> getAllMenus() {
        return menuMapper.toResponseList(menuRepository.findByIsDeletedFalse());
    }

    @Override
    public Menu getById(String id) {
        return menuRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ERROR_MENU_NOT_FOUND, id));
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public MenuResponse create(MenuSaveRequest request) {
        validateMenuCodeUniqueness(request.getCode());

        log.debug("Creating menu: code={}, appCode={}", request.getCode(), request.getAppCode());

        Menu menu = buildMenuFromRequest(request);
        Menu savedMenu = menuRepository.save(menu);

        assignPermissionsToMenu(savedMenu, request.getPermissionIds());

        return menuMapper.toResponse(savedMenu);
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public MenuResponse update(String id, MenuSaveRequest request) {
        Menu menu = getById(id);

        menuMapper.updateEntity(menu, request);
        menu.setFeUrl(request.getFeUrl());
        menu.setFolderPath(request.getFolderPath());

        Menu savedMenu = menuRepository.save(menu);

        updateMenuPermissions(id, savedMenu, request.getPermissionIds());

        return menuMapper.toResponse(savedMenu);
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void delete(String id) {
        Menu menu = getById(id);
        menu.setIsDeleted(true);
        menuRepository.save(menu);
        log.debug("Soft deleted menu: {}", id);
    }

    private void validateMenuCodeUniqueness(String code) {
        if (menuRepository.existsByCodeAndIsDeletedFalse(code)) {
            throw new AppException(ERROR_MENU_CODE_EXISTS, code);
        }
    }

    private Menu buildMenuFromRequest(MenuSaveRequest request) {
        Menu menu = new Menu();
        menu.setCode(request.getCode());
        menu.setName(request.getName());
        menu.setNameEn(request.getNameEn());
        menu.setAppCode(request.getAppCode());
        menu.setParentCode(request.getParentCode());
        menu.setOrderIndex(request.getOrderIndex());
        menu.setMenuType(request.getMenuType());
        menu.setIsPublic(request.getIsPublic());
        menu.setIsDeleted(false);
        menu.setFeUrl(request.getFeUrl());
        menu.setFolderPath(request.getFolderPath());
        menu.setStatus(request.getStatus());
        return menu;
    }

    private void assignPermissionsToMenu(Menu menu, List<String> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }

        List<MenuPermission> newPermissions = permissionIds.stream()
                .map(permissionId -> MenuPermission.builder()
                        .menuId(menu.getId())
                        .permissionId(permissionId)
                        .build())
                .toList();

        menuPermissionRepository.saveAll(newPermissions);
    }

    private void updateMenuPermissions(String menuId, Menu menu, List<String> newPermissionIds) {
        clearExistingPermissions(menuId);
        assignPermissionsToMenu(menu, newPermissionIds);
    }

    private void clearExistingPermissions(String menuId) {
        menuPermissionRepository.deleteByMenuId(menuId);
    }
}
