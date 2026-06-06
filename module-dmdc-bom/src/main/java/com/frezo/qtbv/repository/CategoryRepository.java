package com.frezo.qtbv.repository;

import com.frezo.qtbv.entity.Category;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String>, JpaSpecificationExecutor<Category> {
    boolean existsByCodeAndIsDeletedFalse(String code);

    boolean existsByNameAndIsDeletedFalse(String name);

    boolean existsByNameEnAndIsDeletedFalse(String nameEn);

    Optional<Category> findByCode(String code);

}
