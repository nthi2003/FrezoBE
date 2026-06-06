package com.frezo.qtbv.repository;

import com.frezo.qtbv.common.ReactionType;
import com.frezo.qtbv.entity.ArticleReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleReactionRepository extends JpaRepository<ArticleReaction , String> {
    Optional<ArticleReaction> findByArticleIdAndUserIdAndType(String articleId, String userId,
                                                            ReactionType type);

    Boolean existsByArticleIdAndUserIdAndType(String articleId, String userId,
                                            ReactionType type);

    void deleteByArticleIdAndUserIdAndType(String articleId, String userId,
                                      ReactionType type);

    @Query("SELECT COUNT(r) FROM ArticleReaction r WHERE r.articleId = :articleId")
    Long countByArticleId(String articleId);

}
