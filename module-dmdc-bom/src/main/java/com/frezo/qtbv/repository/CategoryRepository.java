package com.frezo.qtbv.repository;

import com.frezo.qtbv.entity.Category;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String>, JpaSpecificationExecutor<Category> {
    boolean existsByCodeAndIsDeletedFalse(String code);

    boolean existsByCodeAndIsDeletedFalseAndIdNot(String code, String id);

    boolean existsByNameAndIsDeletedFalse(String name);

    boolean existsByNameAndIsDeletedFalseAndIdNot(String name, String id);

    boolean existsByNameEnAndIsDeletedFalse(String nameEn);

    boolean existsByNameEnAndIsDeletedFalseAndIdNot(String nameEn, String id);

    Optional<Category> findByCode(String code);

    Optional<Category> findByGroupCodeAndCodeAndIsDeletedFalse(String groupCode, String code);

}
