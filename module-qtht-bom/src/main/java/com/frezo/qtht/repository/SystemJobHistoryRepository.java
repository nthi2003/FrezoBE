package com.frezo.qtht.repository;

import com.frezo.qtht.entity.SystemJobHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemJobHistoryRepository
        extends JpaRepository<SystemJobHistory, String>, JpaSpecificationExecutor<SystemJobHistory> {
}
