package com.frezo.fbautomation.repository;

import com.frezo.fbautomation.entity.FacebookGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacebookGroupRepository
        extends JpaRepository<FacebookGroup, String>, JpaSpecificationExecutor<FacebookGroup> {

    Optional<FacebookGroup> findByGroupId(String groupId);

    boolean existsByGroupId(String groupId);

    List<FacebookGroup> findByStatus(String status);

    List<FacebookGroup> findByStatusAndRelevanceScoreGreaterThanEqual(String status, Double minScore);
}
