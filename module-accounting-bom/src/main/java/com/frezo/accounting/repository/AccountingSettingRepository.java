package com.frezo.accounting.repository;

import com.frezo.accounting.entity.AccountingSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountingSettingRepository extends JpaRepository<AccountingSetting, String> {

    /** Convention: singleton — luôn lấy record đầu tiên (chưa deleted). */
    Optional<AccountingSetting> findFirstByIsDeletedFalse();
}
