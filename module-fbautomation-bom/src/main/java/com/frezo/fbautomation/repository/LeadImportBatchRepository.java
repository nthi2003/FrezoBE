package com.frezo.fbautomation.repository;

import com.frezo.fbautomation.entity.LeadImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadImportBatchRepository extends JpaRepository<LeadImportBatch, String> {
}
