package com.frezo.qtbv.repository;

import com.frezo.qtbv.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryGroupRepository extends JpaRepository<Category, String>, JpaSpecificationExecutor<Category> {
}
