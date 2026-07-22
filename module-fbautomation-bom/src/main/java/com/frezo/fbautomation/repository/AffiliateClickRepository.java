package com.frezo.fbautomation.repository;

import com.frezo.fbautomation.entity.AffiliateClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface AffiliateClickRepository extends JpaRepository<AffiliateClick, String> {

    List<AffiliateClick> findByLinkId(String linkId);

    long countByLinkIdAndIpAndClickedAtAfter(String linkId, String ip, OffsetDateTime after);

    long countByLinkId(String linkId);
}
