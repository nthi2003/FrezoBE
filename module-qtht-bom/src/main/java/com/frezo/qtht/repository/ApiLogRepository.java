package com.frezo.qtht.repository;

import com.frezo.qtht.entity.ApiLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ApiLogRepository extends JpaRepository<ApiLog, String>, JpaSpecificationExecutor<ApiLog> {

    /** Xoá log cũ hơn cutoff (effFrom &lt; cutoff). */
    @Modifying
    @Query("DELETE FROM ApiLog a WHERE a.effFrom < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
