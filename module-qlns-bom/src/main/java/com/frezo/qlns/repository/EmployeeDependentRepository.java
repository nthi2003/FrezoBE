package com.frezo.qlns.repository;

import com.frezo.qlns.entity.EmployeeDependent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeDependentRepository extends JpaRepository<EmployeeDependent, String> {
    List<EmployeeDependent> findByPersonIdAndIsActiveTrue(String personId);
}
