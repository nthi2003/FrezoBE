package com.frezo.qtht.repository;

import com.frezo.qtht.entity.ApiLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;

public interface ApiLogRepository extends JpaRepository<ApiLog, String> , JpaSpecificationExecutor<ApiLog> {
    void deleteByEffFromAfter(LocalDateTime dateTime);
}
