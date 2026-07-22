package com.frezo.qlns.repository;

import com.frezo.qlns.entity.ContractSignSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContractSignSessionRepository extends JpaRepository<ContractSignSession, String> {
    Optional<ContractSignSession> findFirstByContractIdAndStatusAndIsDeletedFalseOrderByCreatedDateDesc(
            String contractId, String status);

    /** Session mới nhất (mọi status) — dùng GET sign/status. */
    Optional<ContractSignSession> findFirstByContractIdAndIsDeletedFalseOrderByCreatedDateDesc(String contractId);
}
