package com.frezo.qlns.repository;

import com.frezo.qlns.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends JpaRepository<Contract, String>, JpaSpecificationExecutor<Contract> {

    Boolean existsByCode(String code);

}
