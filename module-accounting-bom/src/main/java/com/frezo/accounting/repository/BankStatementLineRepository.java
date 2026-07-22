package com.frezo.accounting.repository;

import com.frezo.accounting.entity.BankStatementLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankStatementLineRepository extends JpaRepository<BankStatementLine, String> {

    List<BankStatementLine> findByStatementIdAndIsDeletedFalseOrderByTxnDateAsc(String statementId);
}
