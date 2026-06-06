package com.frezo.qtbv.repository;

import com.frezo.qtbv.entity.ArticleRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRevisionRepository extends JpaRepository<ArticleRevision, String> {

    List<ArticleRevision> findByArticleIdOrderByCreatedDateDesc(String articleId);

    @Query("SELECT r FROM ArticleRevision r WHERE r.articleId = :articleId ORDER BY r.createdDate DESC")
    List<ArticleRevision> findRevisionsByArticleId(String articleId);

    @Query("SELECT r FROM ArticleRevision r WHERE r.articleId = :articleId ORDER BY r.createdDate DESC")
    List<ArticleRevision> findLastByArticleId(String articleId);
}
