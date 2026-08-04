package com.frezo.qlns.repository;

import com.frezo.qlns.entity.TokenWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TokenWalletRepository extends JpaRepository<TokenWallet, String> {

    Optional<TokenWallet> findByPersonIdAndIsDeletedFalse(String personId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM TokenWallet w WHERE w.personId = :personId AND (w.isDeleted = false OR w.isDeleted IS NULL)")
    Optional<TokenWallet> findForUpdate(@Param("personId") String personId);

    List<TokenWallet> findByIsDeletedFalseOrderByUpdatedDateDesc();
}
