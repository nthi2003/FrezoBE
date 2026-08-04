package com.frezo.crm.repository;

import com.frezo.crm.entity.CommissionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommissionRuleRepository extends JpaRepository<CommissionRule, String> {

    Optional<CommissionRule> findBySalespersonUsernameAndIsDeletedFalse(String salespersonUsername);

    List<CommissionRule> findByIsDeletedFalseOrderBySalespersonUsernameAsc();

    List<CommissionRule> findByActiveTrueAndIsDeletedFalseOrderBySalespersonUsernameAsc();
}
