package com.frezo.fbautomation.repository;

import com.frezo.fbautomation.entity.LivestreamEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface LivestreamEventRepository extends JpaRepository<LivestreamEvent, String> {
    List<LivestreamEvent> findByStatus(String status);
    List<LivestreamEvent> findByStatusAndScheduledAtAfterOrderByScheduledAtAsc(String status, OffsetDateTime after);
    long countByStatus(String status);
}
