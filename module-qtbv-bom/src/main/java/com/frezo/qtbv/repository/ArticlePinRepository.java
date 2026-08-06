package com.frezo.qtbv.repository;

import com.frezo.qtbv.entity.ArticlePin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticlePinRepository extends JpaRepository<ArticlePin, String> {

    List<ArticlePin> findByOrganizationIdAndIsDeletedFalseOrderBySortOrderAsc(String organizationId);

    long countByOrganizationIdAndIsDeletedFalse(String organizationId);

    Optional<ArticlePin> findByOrganizationIdAndArticleIdAndIsDeletedFalse(String organizationId, String articleId);

    List<ArticlePin> findByArticleIdAndIsDeletedFalse(String articleId);
}
