package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.GoodsIssueNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface GoodsIssueNoteRepository extends JpaRepository<GoodsIssueNote, String>, JpaSpecificationExecutor<GoodsIssueNote> {
    Optional<GoodsIssueNote> findByGinCode(String ginCode);

    long countByCreatedDateBetween(LocalDateTime from, LocalDateTime to);
}
