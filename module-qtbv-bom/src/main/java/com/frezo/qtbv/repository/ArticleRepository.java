package com.frezo.qtbv.repository;

import com.frezo.qtbv.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, String>,
                JpaSpecificationExecutor<Article> {
        Optional<Article> findByCode(String code);

        Boolean existsByCode(String code);

        @Query("SELECT a FROM Article a WHERE a.id = :id AND (a.isDeleted = false OR a.isDeleted IS NULL)")
        Article findByIdNotDeleted(@Param("id") String id);

        @Query("SELECT a FROM Article a WHERE a.isDeleted = false OR a.isDeleted IS NULL")
        List<Article> findAllActiveArticles();

        @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' " +
                        "AND a.isActive = true " +
                        "AND a.isPublic = true " +
                        "AND (a.isDeleted = false OR a.isDeleted IS NULL)")
        List<Article> findPublishedArticles();

        @Query("SELECT COUNT(r) FROM ArticleReaction r WHERE r.articleId = :articleId")
        Long countReactionByArticleId(@Param("articleId") String articleId);

        @Query("SELECT r.articleId, COUNT(r) FROM ArticleReaction r WHERE r.articleId IN :articleIds GROUP BY r.articleId")
        List<Object[]> countReactionsByArticleIds(@Param("articleIds") List<String> articleIds);

        @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' " +
                        "AND a.publishScope = 'PUBLIC' " +
                        "AND a.isActive = true " +
                        "AND a.isPublic = true " +
                        "AND (a.isDeleted = false OR a.isDeleted IS NULL) " +
                        "ORDER BY a.publishedAt DESC")
        Page<Article> findPublicArticles(Pageable pageable);

        @Query("SELECT a FROM Article a WHERE a.authorId = :authorId " +
                        "AND (a.isDeleted = false OR a.isDeleted IS NULL) " +
                        "ORDER BY a.createdDate DESC")
        Page<Article> findByAuthorIdAndIsDeletedFalse(@Param("authorId") String authorId, Pageable pageable);

        @Query("SELECT a FROM Article a WHERE a.managerId = :managerId " +
                        "AND a.status = 'WAITING_APPROVAL' " +
                        "AND (a.isDeleted = false OR a.isDeleted IS NULL) " +
                        "ORDER BY a.createdDate DESC")
        Page<Article> findWaitingApprovalByManagerId(@Param("managerId") String managerId, Pageable pageable);
}
