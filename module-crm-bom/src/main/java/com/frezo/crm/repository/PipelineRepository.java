package com.frezo.crm.repository;

import com.frezo.crm.entity.Pipeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PipelineRepository extends JpaRepository<Pipeline, String> {
    List<Pipeline> findByIsDeletedFalseOrderByCreatedDateAsc();
    Optional<Pipeline> findFirstByIsDefaultTrueAndIsDeletedFalse();
}
