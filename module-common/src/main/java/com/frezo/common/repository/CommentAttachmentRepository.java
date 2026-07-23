package com.frezo.common.repository;

import com.frezo.common.entity.CommentAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentAttachmentRepository extends JpaRepository<CommentAttachment, String> {

    List<CommentAttachment> findByCommentIdAndIsDeletedFalseOrderByCreatedDateAsc(String commentId);

    List<CommentAttachment> findByCommentIdInAndIsDeletedFalse(Collection<String> commentIds);

    @Query("""
            SELECT c.subjectId, COUNT(a)
            FROM CommentAttachment a, Comment c
            WHERE a.commentId = c.id
              AND c.subjectType = :subjectType
              AND c.subjectId IN :subjectIds
              AND (a.isDeleted = false OR a.isDeleted IS NULL)
              AND (c.isDeleted = false OR c.isDeleted IS NULL)
            GROUP BY c.subjectId
            """)
    List<Object[]> countBySubjectIds(
            @Param("subjectType") String subjectType,
            @Param("subjectIds") Collection<String> subjectIds);
}
