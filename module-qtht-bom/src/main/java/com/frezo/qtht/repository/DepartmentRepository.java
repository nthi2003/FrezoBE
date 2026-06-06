package com.frezo.qtht.repository;

import com.frezo.qtht.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, String>, JpaSpecificationExecutor<Department> {
        Optional<Department> findByIdAndIsDeletedFalse(String id);
        boolean existsByCode(String code);
}
