package com.frezo.common.repository;

import com.frezo.common.entity.CommentMention;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentMentionRepository extends JpaRepository<CommentMention, String> {

    List<CommentMention> findByCommentIdAndIsDeletedFalse(String commentId);

    void deleteByCommentId(String commentId);
}
