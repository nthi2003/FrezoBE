package com.frezo.fbautomation.repository;

import com.frezo.fbautomation.entity.AffiliateLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AffiliateLinkRepository
        extends JpaRepository<AffiliateLink, String>, JpaSpecificationExecutor<AffiliateLink> {

    Optional<AffiliateLink> findByCode(String code);

    boolean existsByCode(String code);

    List<AffiliateLink> findByCampaign(String campaign);

    List<AffiliateLink> findByStatus(String status);

    /**
     * Atomic increment counter — tránh race condition khi có nhiều click cùng lúc.
     * Denorm cache: `click_count` để dashboard load nhanh mà không join với affiliate_clicks.
     */
    @Modifying
    @Query("UPDATE AffiliateLink a SET a.clickCount = a.clickCount + 1 WHERE a.id = :id")
    void incrementClick(@Param("id") String id);

    @Modifying
    @Query("UPDATE AffiliateLink a SET a.uniqueClickCount = a.uniqueClickCount + 1 WHERE a.id = :id")
    void incrementUniqueClick(@Param("id") String id);

    @Modifying
    @Query("UPDATE AffiliateLink a SET a.conversionCount = a.conversionCount + 1, a.revenue = a.revenue + :value WHERE a.id = :id")
    void incrementConversion(@Param("id") String id, @Param("value") BigDecimal value);
}
