package com.frezo.fbautomation.repository;

import com.frezo.fbautomation.entity.SocialPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface SocialPostRepository
        extends JpaRepository<SocialPost, String>, JpaSpecificationExecutor<SocialPost> {

    /** Query for the scheduler — SCHEDULED posts whose time has come. */
    List<SocialPost> findByStatusAndScheduledAtLessThanEqual(String status, OffsetDateTime cutoff);

    List<SocialPost> findByChannelAndStatus(String channel, String status);

    long countByStatus(String status);
}
