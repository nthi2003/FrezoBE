package com.frezo.qlns.repository;

import com.frezo.qlns.entity.TokenRedeemRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TokenRedeemRequestRepository extends JpaRepository<TokenRedeemRequest, String> {

    List<TokenRedeemRequest> findByIsDeletedFalseOrderByCreatedDateDesc();

    List<TokenRedeemRequest> findByPersonIdAndIsDeletedFalseOrderByCreatedDateDesc(String personId);

    List<TokenRedeemRequest> findByStatusAndIsDeletedFalseOrderByCreatedDateDesc(String status);

    @Query("""
            SELECT r FROM TokenRedeemRequest r
            WHERE (r.isDeleted = false OR r.isDeleted IS NULL)
              AND r.status = :status
              AND r.personId = :personId
              AND r.targetMonth = :month
              AND r.targetYear = :year
            """)
    List<TokenRedeemRequest> findApprovedForPayroll(
            @Param("personId") String personId,
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("status") String status);
}
