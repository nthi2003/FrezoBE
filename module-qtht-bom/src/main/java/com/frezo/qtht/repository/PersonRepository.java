package com.frezo.qtht.repository;

import com.frezo.qtht.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, String>, JpaSpecificationExecutor<Person> {
    Optional<Person> findByEmail(String email);

    Boolean existsByEmail(String email);

    Boolean existsByCode(String code);

    Optional<Person> findByIdAndIsDeletedFalse(String id);

    long countByCreatedDateAfter(LocalDateTime date);

    @Query("""
            SELECT DISTINCT p FROM Person p
            LEFT JOIN FETCH p.department d
            WHERE (p.isDeleted = false OR p.isDeleted IS NULL)
              AND p.activated = true
              AND (:departmentId IS NULL OR :departmentId = '' OR p.departmentId = :departmentId)
              AND (:personId IS NULL OR :personId = '' OR p.id = :personId)
            ORDER BY p.name ASC
            """)
    List<Person> findActiveWithDepartment(
            @Param("departmentId") String departmentId,
            @Param("personId") String personId);

    @Query("""
            SELECT p.id FROM Person p
            WHERE (p.isDeleted = false OR p.isDeleted IS NULL)
              AND p.activated = true
              AND p.departmentId IN :deptIds
              AND p.id <> :excludePersonId
            """)
    List<String> findActivePersonIdsByDepartmentIds(
            @Param("deptIds") java.util.Collection<String> deptIds,
            @Param("excludePersonId") String excludePersonId);
}
