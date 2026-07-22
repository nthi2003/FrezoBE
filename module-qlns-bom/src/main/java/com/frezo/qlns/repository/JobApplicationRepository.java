package com.frezo.qlns.repository;

import com.frezo.qlns.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, String>,
        JpaSpecificationExecutor<JobApplication> {

    Optional<JobApplication> findByCandidateIdAndRequisitionIdAndIsDeletedFalse(
            String candidateId, String requisitionId);

    long countByRequisitionIdAndStageAndIsDeletedFalse(String requisitionId, String stage);

    List<JobApplication> findByRequisitionIdAndIsDeletedFalse(String requisitionId);
}
