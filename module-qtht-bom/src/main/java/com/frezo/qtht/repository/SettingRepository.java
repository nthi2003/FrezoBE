package com.frezo.qtht.repository;

import com.frezo.qtht.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettingRepository extends JpaRepository<Setting, String> {
    Optional<Setting> findByOrgIdAndIsDeletedFalse(String orgId);
    Optional<Setting> findByIdAndIsDeletedFalse(String id);
}
