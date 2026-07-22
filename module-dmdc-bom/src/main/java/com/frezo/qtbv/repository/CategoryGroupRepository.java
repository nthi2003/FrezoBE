package com.frezo.qtbv.repository;

import com.frezo.qtbv.entity.CategoryGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repo cho {@link CategoryGroup}. Trước đây bị typo generic type là {@code Category}
 * — không dùng được, đã fix.
 */
@Repository
public interface CategoryGroupRepository
        extends JpaRepository<CategoryGroup, String>, JpaSpecificationExecutor<CategoryGroup> {
}
