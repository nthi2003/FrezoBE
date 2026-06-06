package com.frezo.qtht.repository;


import com.frezo.qtht.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, String> {
    boolean existsByCodeAndAppCodeAndIsDeletedFalse(String code, String appCode);

    Optional<Role> findByCodeAndAppCodeAndIsDeletedFalse(String roleCode, String appCode);

    List<Role> findByAppCodeAndIsDeletedFalse(String appCode);
    List<Role> findByIsDeletedFalse();

}
