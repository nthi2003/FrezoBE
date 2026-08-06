package com.frezo.qlns.repository;

import com.frezo.qlns.entity.JobPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPositionRepository extends JpaRepository<JobPosition, String>, JpaSpecificationExecutor<JobPosition> {
    List<JobPosition> findByIsDeletedFalseOrderByOrderIndexAscNameAsc();

    long countByIsDeletedFalseAndRankCode(String rankCode);

    long countByIsDeletedFalseAndTitleCode(String titleCode);

    List<JobPosition> findByIsDeletedFalseAndRankCode(String rankCode);

    List<JobPosition> findByIsDeletedFalseAndTitleCode(String titleCode);

    boolean existsByIsDeletedFalseAndRankCode(String rankCode);

    boolean existsByIsDeletedFalseAndTitleCode(String titleCode);
}
