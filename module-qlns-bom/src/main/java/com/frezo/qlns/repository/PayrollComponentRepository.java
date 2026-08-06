package com.frezo.qlns.repository;

import com.frezo.qlns.entity.PayrollComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollComponentRepository extends JpaRepository<PayrollComponent, String> {
    List<PayrollComponent> findByIsDeletedFalseOrderByCodeAsc();

    Optional<PayrollComponent> findByIdAndIsDeletedFalse(String id);

    boolean existsByCodeAndIsDeletedFalse(String code);

    boolean existsByCodeAndIsDeletedFalseAndIdNot(String code, String id);
}
