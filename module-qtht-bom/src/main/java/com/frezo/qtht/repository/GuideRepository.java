package com.frezo.qtht.repository;

import com.frezo.qtht.entity.Guide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuideRepository extends JpaRepository<Guide, String> {

    Optional<Guide> findByIdAndIsDeletedFalse(String id);

    Optional<Guide> findBySlugAndIsDeletedFalse(String slug);

    Optional<Guide> findBySlugAndPublishedTrueAndIsDeletedFalse(String slug);

    boolean existsBySlugAndIsDeletedFalse(String slug);

    boolean existsBySlugAndIsDeletedFalseAndIdNot(String slug, String id);

    List<Guide> findByIsDeletedFalseOrderBySortOrderAscTitleAsc();

    List<Guide> findByPublishedTrueAndIsDeletedFalseOrderBySortOrderAscTitleAsc();
}
