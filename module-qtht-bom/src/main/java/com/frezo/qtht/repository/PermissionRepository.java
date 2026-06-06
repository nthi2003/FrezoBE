package com.frezo.qtht.repository;

import com.frezo.qtht.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {
  Optional<Permission> findByCode(String code);

  @Query(value = """
          SELECT COUNT(p.id) > 0
          FROM permission p
          JOIN role_permission rp ON p.id = rp.permission_id
          JOIN user_role ur ON rp.role_id = ur.role_id
          JOIN users u ON ur.user_id = u.id
          WHERE u.user_name = :username
          AND p.api_path = :apiPath
          AND p.action = :action
      """, nativeQuery = true)
  boolean checkPermission(String username, String apiPath, String action);
}
