package com.frezo.qlns.repository;

import com.frezo.qlns.entity.Requisition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RequisitionRepository extends JpaRepository<Requisition, String>,
        JpaSpecificationExecutor<Requisition> {
}
