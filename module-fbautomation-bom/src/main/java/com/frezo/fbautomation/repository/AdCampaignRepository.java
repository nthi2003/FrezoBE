package com.frezo.fbautomation.repository;

import com.frezo.fbautomation.entity.AdCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdCampaignRepository extends JpaRepository<AdCampaign, String> {
    List<AdCampaign> findByStatus(String status);
    long countByStatus(String status);
}
