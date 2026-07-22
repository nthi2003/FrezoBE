package com.frezo.accounting.repository;

import com.frezo.accounting.common.AccountingStandard;
import com.frezo.accounting.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByCode(String code);

    Optional<Account> findByCodeAndIsDeletedFalse(String code);

    boolean existsByCode(String code);

    List<Account> findByStandardAndIsDeletedFalseOrderByCodeAsc(AccountingStandard standard);

    List<Account> findByIsDeletedFalseOrderByCodeAsc();

    List<Account> findByParentIdAndIsDeletedFalse(String parentId);

    long countByStandard(AccountingStandard standard);
}
