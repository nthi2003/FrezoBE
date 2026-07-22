package com.frezo.qtht.repository;

import com.frezo.qtht.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, String> {

    List<RolePermission> findByRoleIdAndIsDeletedFalse(String roleId);

    List<RolePermission> findByPermissionIdAndIsDeletedFalse(String permissionId);

    boolean existsByRoleIdAndPermissionIdAndIsDeletedFalse(String roleId, String permissionId);

    @Modifying
    @Query("delete from RolePermission rp where rp.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") String roleId);

    @Modifying
    @Query("update RolePermission rp set rp.isDeleted = true where rp.roleId = :roleId")
    void softDeleteByRoleId(@Param("roleId") String roleId);
}
