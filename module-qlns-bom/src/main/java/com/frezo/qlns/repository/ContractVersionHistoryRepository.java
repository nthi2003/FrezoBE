package com.frezo.qlns.repository;

import com.frezo.qlns.entity.ContractVersionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContractVersionHistoryRepository  extends JpaRepository<ContractVersionHistory, String>, JpaSpecificationExecutor<ContractVersionHistory> {

    @Query("SELECT MAX(c.versionNumber) FROM ContractVersionHistory c WHERE c.contract.id = :contractId")
    Integer findMaxVersionByContractId(@Param("contractId") String contractId);

    @Query("SELECT c FROM ContractVersionHistory c WHERE c.contract.id = :contractId ORDER BY c.versionNumber DESC")
    List<ContractVersionHistory> findAllByContractIdOrderByVersionDesc(@Param("contractId") String contractId);

    @Query("SELECT c FROM ContractVersionHistory c WHERE c.contract.id = :contractId AND c.versionNumber = :versionNumber")
    Optional<ContractVersionHistory> findByContractIdAndVersionNumber(@Param("contractId") String contractId, @Param("versionNumber") Integer versionNumber);
}
