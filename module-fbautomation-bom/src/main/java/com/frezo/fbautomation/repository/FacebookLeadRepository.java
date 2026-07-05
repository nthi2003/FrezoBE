package com.frezo.fbautomation.repository;

import com.frezo.fbautomation.entity.FacebookLead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacebookLeadRepository
        extends JpaRepository<FacebookLead, String>, JpaSpecificationExecutor<FacebookLead> {

    List<FacebookLead> findByStatus(String status);

    List<FacebookLead> findBySourceGroupId(String sourceGroupId);

    boolean existsByProfileUrl(String profileUrl);

    long countByStatus(String status);
}
