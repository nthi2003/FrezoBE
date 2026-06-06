package com.frezo.auth.repository;

import com.frezo.auth.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, String> {
    boolean existsByUserIdAndRoleId(String userId, String roleId);

    List<UserRole> findByUserId(String userId);

    List<UserRole> findByUserIdAndIsDeletedFalse(String userId);
}
