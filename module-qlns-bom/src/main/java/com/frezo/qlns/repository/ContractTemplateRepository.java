package com.frezo.qlns.repository;

import com.frezo.qlns.entity.ContractTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, String> {
    List<ContractTemplate> findByIsDeletedFalseOrderByCreatedDateDesc();
    Optional<ContractTemplate> findByIdAndIsDeletedFalse(String id);
}
