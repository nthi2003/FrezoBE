package com.frezo.qtht.repository;

import com.frezo.qtht.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {


    @Query(value = """
            SELECT COUNT(p.id) > 0
            FROM permission p
            JOIN role_permission rp ON p.id = rp.permission_id
            JOIN roles r            ON rp.role_id = r.id
            JOIN user_role ur       ON r.id = ur.role_id
            JOIN users u            ON ur.user_id = u.id
            WHERE u.user_name = :username
              AND (
                    p.api_path = :apiPath
                 OR p.api_path = LTRIM(:apiPath, '/')
                 OR '/' || LTRIM(p.api_path, '/') = :apiPath
              )
              AND p.action    = :action
              AND p.is_deleted = false
              AND (r.is_deleted = false OR r.is_deleted IS NULL)
              AND (r.status = 'A' OR r.status IS NULL)
              AND (ur.is_deleted = false OR ur.is_deleted IS NULL)
              AND (u.is_deleted = false OR u.is_deleted IS NULL)
              AND (u.status = 1 OR u.status IS NULL)
        """, nativeQuery = true)
    boolean checkPermission(@Param("username") String username,
                            @Param("apiPath") String apiPath,
                            @Param("action") String action);


    @Query(value = """
            SELECT DISTINCT p.code
            FROM permission p
            JOIN role_permission rp ON p.id = rp.permission_id
            JOIN roles r            ON rp.role_id = r.id
            JOIN user_role ur       ON r.id = ur.role_id
            JOIN users u            ON ur.user_id = u.id
            WHERE u.user_name = :username
              AND p.is_deleted = false
              AND (rp.is_deleted = false OR rp.is_deleted IS NULL)
              AND (r.is_deleted = false OR r.is_deleted IS NULL)
              AND (r.status = 'A' OR r.status IS NULL)
              AND (ur.is_deleted = false OR ur.is_deleted IS NULL)
              AND (u.is_deleted = false OR u.is_deleted IS NULL)
              AND (u.status = 1 OR u.status IS NULL)
            ORDER BY p.code
        """, nativeQuery = true)
    List<String> findCodesByUsername(@Param("username") String username);
}
