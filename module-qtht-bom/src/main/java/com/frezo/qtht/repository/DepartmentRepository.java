package com.frezo.qtht.repository;

import com.frezo.qtht.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, String>, JpaSpecificationExecutor<Department> {
        Optional<Department> findByIdAndIsDeletedFalse(String id);
        boolean existsByCode(String code);

        @org.springframework.data.jpa.repository.Query("""
                        SELECT d FROM Department d
                        WHERE (d.isDeleted = false OR d.isDeleted IS NULL)
                          AND (d.managerId = :personId OR d.deputyManagerId = :personId)
                        """)
        List<Department> findManagedByPersonId(@org.springframework.data.repository.query.Param("personId") String personId);

        @org.springframework.data.jpa.repository.Query("""
                        SELECT d FROM Department d
                        WHERE (d.isDeleted = false OR d.isDeleted IS NULL)
                          AND d.path LIKE CONCAT(:pathPrefix, '%')
                        """)
        List<Department> findByPathStartingWithAndIsDeletedFalse(
                        @org.springframework.data.repository.query.Param("pathPrefix") String pathPrefix);
}
