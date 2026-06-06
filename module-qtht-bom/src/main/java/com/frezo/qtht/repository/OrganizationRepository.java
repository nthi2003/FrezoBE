package com.frezo.qtht.repository;

import com.frezo.qtht.entity.Organization;
import com.frezo.qtht.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository
        extends JpaRepository<Organization, String>, JpaSpecificationExecutor<Organization> {

    Boolean existsByCode(String code);

    Boolean existsByTaxCode(String taxCode);

    Optional<Organization> findByCode(String code);

    Optional<Organization> findByIdAndIsDeletedFalse(String id);

    List<Organization> findByParentIdAndIsDeletedFalse(String parentId);

    @Query("SELECT o FROM Organization o LEFT JOIN FETCH o.parent WHERE o.id = :id AND o.isDeleted = false")
    Optional<Organization> findByIdWithParent(@Param("id") String id);
}
