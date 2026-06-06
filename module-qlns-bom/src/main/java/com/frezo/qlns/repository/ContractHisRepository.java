package com.frezo.qlns.repository;

import com.frezo.qlns.entity.ContractHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractHisRepository extends JpaRepository<ContractHistory, String>, JpaSpecificationExecutor<ContractHistory> {
}
