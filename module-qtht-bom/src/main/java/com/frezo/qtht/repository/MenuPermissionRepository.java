package com.frezo.qtht.repository;

import com.frezo.qtht.entity.MenuPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuPermissionRepository extends JpaRepository<MenuPermission, String> {
    List<MenuPermission> findByMenuId(String menuId);

    void deleteByMenuId(String menuId);
}
