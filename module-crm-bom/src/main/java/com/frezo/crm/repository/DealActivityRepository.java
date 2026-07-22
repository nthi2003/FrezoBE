package com.frezo.crm.repository;

import com.frezo.crm.entity.DealActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DealActivityRepository extends JpaRepository<DealActivity, String> {
    List<DealActivity> findByDealIdAndIsDeletedFalseOrderByHappenedAtDesc(String dealId);
    List<DealActivity> findByCustomerIdAndIsDeletedFalseOrderByHappenedAtDesc(String customerId);
}
