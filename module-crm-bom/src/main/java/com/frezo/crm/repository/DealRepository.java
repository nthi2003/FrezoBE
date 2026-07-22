package com.frezo.crm.repository;

import com.frezo.crm.common.DealStatus;
import com.frezo.crm.entity.Deal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DealRepository extends JpaRepository<Deal, String> {
    List<Deal> findByPipelineIdAndIsDeletedFalseOrderByCreatedDateDesc(String pipelineId);
    List<Deal> findByStageIdAndIsDeletedFalseOrderByCreatedDateDesc(String stageId);
    List<Deal> findByStatusAndIsDeletedFalseOrderByCreatedDateDesc(DealStatus status);
    List<Deal> findByOwnerUsernameAndIsDeletedFalseOrderByCreatedDateDesc(String owner);
    List<Deal> findByCustomerIdAndIsDeletedFalseOrderByCreatedDateDesc(String customerId);
}
