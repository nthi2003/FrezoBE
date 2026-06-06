package com.frezo.qlns.repository;

import com.frezo.qlns.entity.ContractAssginWork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractAssginWorkReposirory extends JpaRepository<ContractAssginWork, String>, JpaSpecificationExecutor<ContractAssginWork> {

    ContractAssginWork findByContractId(String contractId);
}
