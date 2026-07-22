package com.frezo.accounting.repository;

import com.frezo.accounting.entity.BankStatement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankStatementRepository extends JpaRepository<BankStatement, String> {

    List<BankStatement> findByIsDeletedFalseOrderByImportedAtDesc();
}
