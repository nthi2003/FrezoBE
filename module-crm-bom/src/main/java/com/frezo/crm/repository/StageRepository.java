package com.frezo.crm.repository;

import com.frezo.crm.entity.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StageRepository extends JpaRepository<Stage, String> {
    List<Stage> findByPipelineIdAndIsDeletedFalseOrderByOrderNoAsc(String pipelineId);
}
