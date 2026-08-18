package com.frezo.profit.repository;

import com.frezo.profit.entity.ProfitSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfitSettingRepository extends JpaRepository<ProfitSetting, String> {

    List<ProfitSetting> findByIsDeletedFalseOrderByScopeAscUpdatedDateDesc();

    Optional<ProfitSetting> findByScopeAndScopeRefIdAndIsDeletedFalse(String scope, String scopeRefId);

    List<ProfitSetting> findByScopeAndIsDeletedFalse(String scope);
}
