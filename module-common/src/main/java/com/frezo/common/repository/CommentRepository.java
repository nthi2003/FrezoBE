package com.frezo.common.repository;

import com.frezo.common.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, String> {

    Page<Comment> findBySubjectTypeAndSubjectIdAndIsDeletedFalseOrderByCreatedDateAsc(
            String subjectType, String subjectId, Pageable pageable);

    /** User comments only (exclude soft-deleted + system activity). */
    @Query("""
            SELECT COUNT(c) FROM Comment c
            WHERE c.subjectType = :subjectType
              AND c.subjectId = :subjectId
              AND (c.isDeleted = false OR c.isDeleted IS NULL)
              AND (c.isSystem = false OR c.isSystem IS NULL)
            """)
    long countUserComments(
            @Param("subjectType") String subjectType,
            @Param("subjectId") String subjectId);

    @Query("""
            SELECT c.subjectId, COUNT(c)
            FROM Comment c
            WHERE c.subjectType = :subjectType
              AND c.subjectId IN :subjectIds
              AND (c.isDeleted = false OR c.isDeleted IS NULL)
              AND (c.isSystem = false OR c.isSystem IS NULL)
            GROUP BY c.subjectId
            """)
    List<Object[]> countUserCommentsGrouped(
            @Param("subjectType") String subjectType,
            @Param("subjectIds") Collection<String> subjectIds);
}
