package com.frezo.crm.repository;

import com.frezo.crm.common.LeadStatus;
import com.frezo.crm.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, String> {
    List<Lead> findByIsDeletedFalseOrderByCreatedDateDesc();
    List<Lead> findByStatusAndIsDeletedFalseOrderByCreatedDateDesc(LeadStatus status);
    List<Lead> findByOwnerUsernameAndIsDeletedFalseOrderByCreatedDateDesc(String owner);
}
