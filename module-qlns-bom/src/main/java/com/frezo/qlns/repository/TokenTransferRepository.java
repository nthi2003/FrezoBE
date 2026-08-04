package com.frezo.qlns.repository;

import com.frezo.qlns.entity.TokenTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TokenTransferRepository extends JpaRepository<TokenTransfer, String> {

    List<TokenTransfer> findByIsDeletedFalseOrderByCreatedDateDesc();

    @Query("""
            SELECT t FROM TokenTransfer t
            WHERE (t.isDeleted = false OR t.isDeleted IS NULL)
              AND (:personId IS NULL OR t.fromPersonId = :personId OR t.toPersonId = :personId)
            ORDER BY t.createdDate DESC
            """)
    List<TokenTransfer> findHistory(@Param("personId") String personId);
}
