package com.frezo.auth.repository;

import com.frezo.auth.entity.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, String> {
    List<LoginHistory> findByUserNameOrderByLoginTimeDesc(String userName);

    List<LoginHistory> findTop3ByUserNameOrderByLoginTimeDesc(String userName);

    @Query("SELECT COUNT(h) FROM LoginHistory h WHERE h.status = 'SUCCESS' AND h.loginTime >= :from AND h.loginTime < :to")
    long countSuccessBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(DISTINCT h.userName) FROM LoginHistory h WHERE h.status = 'SUCCESS' AND h.loginTime >= :from AND h.loginTime < :to")
    long countDistinctUsersSuccessBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<LoginHistory> findByStatusAndLoginTimeGreaterThanEqual(String status, LocalDateTime from);
}
